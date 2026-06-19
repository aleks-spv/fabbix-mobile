package com.fabbixmb.app.data.local

import androidx.room.*

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val username: String,
    val hasPassword: Boolean,
    val ignoreSsl: Boolean,
    val sortOrder: Int = 0
)
