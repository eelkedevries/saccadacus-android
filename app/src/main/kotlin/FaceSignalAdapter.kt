package com.example.saccadacusandroid

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Converts a MediaPipe [FaceLandmarkerResult] into derived [TrackingFrameResult] signals
 * (prompt 005): eye-local iris position, blink state, and head pose. Mirroring / eye
 * identity / sign conventions live in [SignConvention] and need on-device confirmation.
 */
object FaceSignalAdapter {

    private val blinkClassifier = BlinkClassifier()

    fun toResult(result: FaceLandmarkerResult): TrackingFrameResult {
        val faces = result.faceLandmarks()
        if (faces.isEmpty()) {
            blinkClassifier.reset()
            return TrackingFrameResult(false, 0f, null, null, null)
        }
        val landmarks = faces[0]

        var blinkLeft = 0f
        var blinkRight = 0f
        // Eye-look blendshapes (anatomical, per eye) for the blendshape gaze source (030).
        var lookInL = 0f; var lookOutL = 0f; var lookUpL = 0f; var lookDownL = 0f
        var lookInR = 0f; var lookOutR = 0f; var lookUpR = 0f; var lookDownR = 0f
        val blendshapes = result.faceBlendshapes()
        if (blendshapes.isPresent && blendshapes.get().isNotEmpty()) {
            for (category in blendshapes.get()[0]) {
                when (category.categoryName()) {
                    "eyeBlinkLeft" -> blinkLeft = category.score()
                    "eyeBlinkRight" -> blinkRight = category.score()
                    "eyeLookInLeft" -> lookInL = category.score()
                    "eyeLookOutLeft" -> lookOutL = category.score()
                    "eyeLookUpLeft" -> lookUpL = category.score()
                    "eyeLookDownLeft" -> lookDownL = category.score()
                    "eyeLookInRight" -> lookInR = category.score()
                    "eyeLookOutRight" -> lookOutR = category.score()
                    "eyeLookUpRight" -> lookUpR = category.score()
                    "eyeLookDownRight" -> lookDownR = category.score()
                }
            }
        }

        // Per-eye blink state, adaptive to each eye's open baseline (prompt 034). The pairing of
        // eyeBlinkLeft/Right to the participant eyes is preserved from the score-based version.
        val blinkStateLeft = blinkClassifier.classifyLeft(blinkLeft)
        val blinkStateRight = blinkClassifier.classifyRight(blinkRight)

        val participantLeft: EyeFeature?
        val participantRight: EyeFeature?
        if (SessionConfig.signalSource == SessionConfig.SOURCE_BLENDSHAPE) {
            // Anatomical, participant frame: +x = participant's right, +y = up. For the left
            // eye, nasal (in) = toward participant-right; for the right eye, temporal (out) = right.
            participantLeft = EyeFeature(lookInL - lookOutL, lookUpL - lookDownL, 1f, blinkStateLeft)
            participantRight = EyeFeature(lookOutR - lookInR, lookUpR - lookDownR, 1f, blinkStateRight)
        } else {
            val imgLeftEye = eyeFeature(
                landmarks,
                FaceMeshIndices.IMG_LEFT_EYE_CORNER_OUTER,
                FaceMeshIndices.IMG_LEFT_EYE_CORNER_INNER,
                FaceMeshIndices.IMG_LEFT_IRIS_CENTRE,
                blinkStateLeft,
            )
            val imgRightEye = eyeFeature(
                landmarks,
                FaceMeshIndices.IMG_RIGHT_EYE_CORNER_INNER,
                FaceMeshIndices.IMG_RIGHT_EYE_CORNER_OUTER,
                FaceMeshIndices.IMG_RIGHT_IRIS_CENTRE,
                blinkStateRight,
            )
            // Under a mirrored front camera, the image-left eye is the participant's right eye.
            participantLeft = if (SignConvention.MIRROR_X) imgRightEye else imgLeftEye
            participantRight = if (SignConvention.MIRROR_X) imgLeftEye else imgRightEye
        }

        return TrackingFrameResult(true, 1f, participantLeft, participantRight, headPose(result))
    }

    private fun eyeFeature(
        landmarks: List<NormalizedLandmark>,
        cornerAIndex: Int,
        cornerBIndex: Int,
        irisIndex: Int,
        blinkState: BlinkState,
    ): EyeFeature? {
        if (irisIndex >= landmarks.size) return null
        val a = participantPoint(landmarks[cornerAIndex])
        val b = participantPoint(landmarks[cornerBIndex])
        val iris = participantPoint(landmarks[irisIndex])
        // Order so leftCorner is participant-left (smaller x), rightCorner participant-right.
        val corners = if (a.x <= b.x) EyeCorners(a, b) else EyeCorners(b, a)
        val local = projectEyeLocal(corners, iris)
        return EyeFeature(local.xLocal, local.yLocal, 1f, blinkState)
    }

    private fun participantPoint(landmark: NormalizedLandmark): Point2D {
        val x = if (SignConvention.MIRROR_X) 1f - landmark.x() else landmark.x()
        val y = if (SignConvention.FLIP_Y) 1f - landmark.y() else landmark.y()
        return Point2D(x, y)
    }

    /** Approximate yaw/pitch/roll from the 4x4 (row-major) facial transformation matrix. */
    private fun headPose(result: FaceLandmarkerResult): HeadPose? {
        val matrices = result.facialTransformationMatrixes()
        if (!matrices.isPresent || matrices.get().isEmpty()) return null
        val m = matrices.get()[0]
        if (m.size < 16) return null
        val yaw = Math.toDegrees(atan2(m[8].toDouble(), m[10].toDouble())).toFloat()
        val pitch = Math.toDegrees(asin((-m[9]).toDouble().coerceIn(-1.0, 1.0))).toFloat()
        val roll = Math.toDegrees(atan2(m[1].toDouble(), m[5].toDouble())).toFloat()
        return HeadPose(yaw, pitch, roll)
    }
}
