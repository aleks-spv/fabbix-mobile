package com.fabbixmb.app.domain.model

data class Trigger(
    val triggerId: String,
    val description: String,
    val priority: Severity,
    val status: Boolean,
    val hosts: List<String>
)