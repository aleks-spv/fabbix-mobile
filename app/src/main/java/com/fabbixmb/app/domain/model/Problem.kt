package com.fabbixmb.app.domain.model

data class ProblemTag(
    val tag: String,
    val value: String
)

data class ProblemHost(
    val hostId: String,
    val name: String,
    val ip: String = "",
    val availability: HostAvailability = HostAvailability.UNKNOWN
)

data class Problem(
    val eventId: String,
    val objectId: String,
    val name: String,
    val severity: Severity,
    val acknowledged: Boolean,
    val clock: Long,
    val resolvedClock: Long?,
    val hosts: List<ProblemHost> = emptyList(),
    val tags: List<ProblemTag> = emptyList(),
    val triggerDescription: String = "",
    val triggerExpression: String = "",
    val opdata: String = ""
) {
    val isResolved: Boolean get() = resolvedClock != null && resolvedClock > 0
}