package com.fabbixmb.app.domain.model

data class Host(
    val hostId: String,
    val technicalName: String,
    val visibleName: String,
    val enabled: Boolean,
    val available: HostAvailability,
    val interfaces: List<HostInterface> = emptyList(),
    val templates: List<String> = emptyList(),
    val macros: List<HostMacro> = emptyList(),
    val monitoredBy: String = ""
)

data class HostMacro(
    val macro: String,
    val value: String
)

enum class HostAvailability(val id: Int) {
    UNKNOWN(0), AVAILABLE(1), UNAVAILABLE(2);
    companion object {
        fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}

data class HostInterface(
    val ip: String,
    val dns: String,
    val type: Int
)