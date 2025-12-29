package com.example.mindarc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restricted_apps")
data class RestrictedApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val iconUri: String? = null
)

