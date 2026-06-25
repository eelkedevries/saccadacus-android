package com.example.saccadacusandroid

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "saccadacus_settings")

/**
 * Persists user settings across launches (prompt 025) via Preferences DataStore. The
 * in-memory config objects remain the runtime source of truth; [load] hydrates them at
 * startup and [save] mirrors them (called when the app is backgrounded).
 */
object AppSettings {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val PROFILE = stringPreferencesKey("profile")
    private val USE_CASE = stringPreferencesKey("use_case_mode")
    private val EYE_MODE = stringPreferencesKey("eye_mode")
    private val RAW_VIDEO = booleanPreferencesKey("raw_video_enabled")
    private val OVERLAY = booleanPreferencesKey("overlay_enabled")
    private val FILTER = booleanPreferencesKey("filter_enabled")
    private val SESSION_NAME = stringPreferencesKey("session_name")
    private val SESSION_NOTE = stringPreferencesKey("session_note")

    /** Hydrate the in-memory config from disk. Blocking, but a tiny read at startup. */
    fun load(context: Context) {
        val prefs = try {
            runBlocking { context.settingsDataStore.data.first() }
        } catch (t: Throwable) {
            return
        }
        prefs[PROFILE]?.let { name -> ProbeConfig.profiles.find { it.name == name }?.let { ProbeConfig.selected = it } }
        prefs[USE_CASE]?.let { SessionConfig.useCaseMode = it }
        prefs[EYE_MODE]?.let { SessionConfig.eyeMode = it }
        prefs[RAW_VIDEO]?.let { SessionConfig.rawVideoEnabled = it }
        prefs[OVERLAY]?.let { OverlayConfig.enabled = it }
        prefs[FILTER]?.let { SessionConfig.filterEnabled = it }
        prefs[SESSION_NAME]?.let { SessionConfig.sessionName = it }
        prefs[SESSION_NOTE]?.let { SessionConfig.sessionNote = it }
    }

    /** Mirror the current in-memory config to disk (fire-and-forget on an IO scope). */
    fun save(context: Context) {
        scope.launch {
            try {
                context.settingsDataStore.edit { p ->
                    p[PROFILE] = ProbeConfig.selected.name
                    p[USE_CASE] = SessionConfig.useCaseMode
                    p[EYE_MODE] = SessionConfig.eyeMode
                    p[RAW_VIDEO] = SessionConfig.rawVideoEnabled
                    p[OVERLAY] = OverlayConfig.enabled
                    p[FILTER] = SessionConfig.filterEnabled
                    p[SESSION_NAME] = SessionConfig.sessionName
                    p[SESSION_NOTE] = SessionConfig.sessionNote
                }
            } catch (t: Throwable) {
                // best-effort persistence
            }
        }
    }
}
