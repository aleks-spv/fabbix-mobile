package com.fabbixmb.app.domain.model

data class Problem(
    val eventId: String,
    val objectId: String,
    val name: String,
    val severity: Severity,
    val acknowledged: Boolean,
    val clock: Long,
    val resolvedClock: Long?,
    val hosts: List<String> = emptyList()
) {
    val isResolved: Boolean get() = resolvedClock != null && resolvedClock > 0
}