package com.armstyle.customchatbot.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_history_table")
data class ChatMessage (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name="chat")
    var chat: String? = "",

    @ColumnInfo(name="user")
    var user: String? = ""
)