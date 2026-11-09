package com.redmemory.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val kind: String = "TEXT",
    @ColumnInfo(defaultValue = "''") val attachments: String = "",
    @ColumnInfo(defaultValue = "'GENERAL'") val category: String = "GENERAL"
)
