package com.nutriai.domain.activity

/** Protocolos de ayuno intermitente: horas de ayuno y de ventana de alimentación. */
enum class FastingProtocol(val fastingHours: Int) {
    P12_12(12), P14_10(14), P16_8(16), P18_6(18), P20_4(20);

    val eatingHours: Int get() = 24 - fastingHours
    val label: String get() = "$fastingHours:$eatingHours"
}

data class FastingSession(
    val id: Long = 0,
    val profileId: Long,
    val protocol: FastingProtocol,
    val startEpochMs: Long,
    val plannedEndEpochMs: Long,
    val endEpochMs: Long? = null,
) {
    val isActive: Boolean get() = endEpochMs == null

    fun elapsedMs(nowMs: Long): Long = ((endEpochMs ?: nowMs) - startEpochMs).coerceAtLeast(0)

    fun progress(nowMs: Long): Float {
        val total = (plannedEndEpochMs - startEpochMs).coerceAtLeast(1)
        return (elapsedMs(nowMs).toFloat() / total).coerceIn(0f, 1f)
    }

    fun remainingMs(nowMs: Long): Long = (plannedEndEpochMs - nowMs).coerceAtLeast(0)

    /** Se considera completado si duró al menos el tiempo previsto. */
    val completed: Boolean get() = endEpochMs != null && endEpochMs >= plannedEndEpochMs

    companion object {
        fun start(profileId: Long, protocol: FastingProtocol, startEpochMs: Long) = FastingSession(
            profileId = profileId,
            protocol = protocol,
            startEpochMs = startEpochMs,
            plannedEndEpochMs = startEpochMs + protocol.fastingHours * 3_600_000L,
        )
    }
}
