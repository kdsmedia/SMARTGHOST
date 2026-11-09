package com.altomedia.smartghost.data.diagnostics

/** Diagnostic status for one of the ten cards (Bab 8). Never a "viewer" count. */
enum class DiagnosticStatus {
    /** Belum diuji */
    NOT_TESTED,

    /** Sedang berjalan */
    RUNNING,

    /** Berhasil */
    SUCCESS,

    /** Gagal */
    FAILURE,
}

/**
 * One diagnostic result for a single authorised test endpoint. Each of the ten
 * cards shows exactly one of these — it reports connectivity, latency and an
 * error summary, never a claim about YouTube viewers (Bab 7, 8).
 */
data class DiagnosticResult(
    val index: Int,
    val endpointLabel: String,
    val status: DiagnosticStatus = DiagnosticStatus.NOT_TESTED,
    val latencyMs: Long? = null,
    val httpCode: Int? = null,
    val lastCheckedAt: Long? = null,
    val errorSummary: String? = null,
) {
    companion object {
        fun empty(index: Int, label: String) = DiagnosticResult(
            index = index,
            endpointLabel = label,
        )
    }
}
