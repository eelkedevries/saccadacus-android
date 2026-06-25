package com.example.saccadacusandroid

/** One row of the gaze-model comparison shown in the in-app fold-out (prompt 045). */
data class GazeModelInfo(
    val name: String,
    val publisher: String,
    val license: String,
    val year: String,
    val accuracy: String,
    val size: String,
    val pros: String,
    val cons: String,
)

/**
 * Static reference comparison of on-device gaze-estimation models (prompt 045), surfaced in-app so
 * candidates can be compared on the device. A snapshot from this project's research scout — **every
 * listed model's weights are non-commercial / research-only (dataset-tainted)**; `license` is the
 * *code* licence. Verify details upstream before relying on them.
 */
object GazeModels {
    val all: List<GazeModelInfo> = listOf(
        GazeModelInfo(
            "WebEyeTrack / BlazeGaze", "RedForestAI (Vanderbilt)", "MIT (code)", "2025", "~2.0-2.3 cm", "0.67 MB",
            "Our exact stack (MediaPipe + eye patch + head pose + few-shot); tiny",
            "Newest/least-tested; weights GazeCapture/MPII-tainted",
        ),
        GazeModelInfo(
            "MPIIGaze (hysts/ptgaze)", "MPI-Inf; impl. hysts", "MIT (code)", "2015 / ~2021", "~4.5-6 deg", "~few MB",
            "Matches our [1,36,60,1] eye-patch contract exactly; canonical", "Older accuracy; weights CC BY-NC-SA",
        ),
        GazeModelInfo(
            "MobileGaze (yakhyo)", "indie (Y. Kholmatov)", "MIT (code)", "~2024", "11-13 deg (Gaze360)", "4.8 MB (S0)",
            "Best on-device speed/size; weights downloadable", "Full-face (not our contract); Gaze360-tainted",
        ),
        GazeModelInfo(
            "L2CS-Net", "Abdelrahman et al.", "MIT (code)", "2022", "3.92 deg (MPIIGaze)", "~96 MB",
            "Most accurate here; pre-converted ONNX exists", "Heavy ResNet-50; full-face; tainted",
        ),
        GazeModelInfo(
            "GazeML / ELG", "Park et al. (ETH)", "MIT (code)", "2018", "Modest", "Small",
            "Cleanest licence (synthetic UnityEyes); eye-sized", "TF1-era; indirect gaze; murky weight provenance",
        ),
        GazeModelInfo(
            "s0mnaths Gaze-Tracker", "s0mnaths (GSoC)", "none stated", "~2021", "1.0-2.0 cm", "mobile-small",
            "Mobile iTracker-style; cm metric like ours", "No licence; GazeCapture-tainted; no .tflite shipped",
        ),
        GazeModelInfo(
            "ETH-XGaze", "Zhang et al. (ETH)", "CC BY-NC-SA (code!)", "2020", "~4-4.5 deg", "~90 MB",
            "Strong head-pose normalisation; documented", "Code and data non-commercial; heavy; full-face",
        ),
        GazeModelInfo(
            "FAZE", "NVlabs", "NVIDIA SCL (non-comm.)", "2019", "3.18 deg @ 3-shot", "Moderate",
            "Best few-shot personalisation idea", "Hard NC code licence; complex (MAML); tainted",
        ),
        GazeModelInfo(
            "iTracker / GazeCapture", "MIT CSAIL", "Custom research-only", "2016", "1.3-2.0 cm", "~8 M params",
            "The original mobile-gaze benchmark", "Research-only (code+data+weights); Caffe; dated",
        ),
        GazeModelInfo(
            "UniGaze", "ut-vision", "ModelGo MG-BY-NC (non-comm.)", "2025", "SOTA cross-dataset", "ViT (large)",
            "Best generalisation across datasets", "ViT too heavy for mid-range; explicitly NC + triple-tainted",
        ),
        GazeModelInfo(
            "TabletGaze", "Rice Univ (Huang et al.)", "Academic", "2016", "2.5-3.2 cm", "Small (HoG+RF)",
            "Calibration-free option; classic baseline", "2016 HoG+Random-Forest (not deep); modest",
        ),
        GazeModelInfo(
            "OpenFace 2.0 (gaze)", "CMU (Baltrusaitis)", "CMU academic-only (paid commercial)", "2018", "~few deg", "Toolkit",
            "Mature facial-behaviour toolkit", "Desktop C++; bespoke NC licence; dataset-tainted",
        ),
        GazeModelInfo(
            "OpenGaze", "Univ. Stuttgart (Bulling grp)", "Research (requires OpenFace)", "2019", "~few deg", "Toolkit",
            "Research toolkit for MPIIFaceGaze", "Requires OpenFace (NC); desktop; tainted",
        ),
        GazeModelInfo(
            "Open Gaze (replication)", "open replication (arXiv 2308.13495)", "code (repo)", "2023", "~1.9 cm", "small",
            "Open replication of Google's 0.46 cm approach", "GazeCapture-tainted; worse than the original",
        ),
        GazeModelInfo(
            "GAZEL", "joonb14", "unclear", "~2021", "n/a", "n/a",
            "Android TFLite gaze framework example", "No released weights - train-your-own scaffold only",
        ),
        GazeModelInfo(
            "WalidAlHassan/Gaze-Estimation", "indie (Hugging Face)", "unknown", "recent", "n/a", "n/a",
            "Listed for completeness", "Gated repo, no metadata/files - not usable",
        ),
        GazeModelInfo(
            "MediaPipe Iris", "Google", "Apache-2.0", "2020", "n/a", "tiny",
            "On-device, clean licence, already in our stack", "Does NOT output gaze - landmark building block only",
        ),
        GazeModelInfo(
            "Google smartphone model", "Google (Nature Comms)", "unreleased", "2020", "0.46 cm (best anywhere)", "170 K params",
            "The accuracy ceiling for phone gaze", "Never released - no code/weights, unobtainable",
        ),
    )
}
