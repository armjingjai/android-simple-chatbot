package com.armstyle.customchatbot.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.armstyle.customchatbot.vo.ChatMessage

@Database(entities = [ChatMessage::class],version = 1 ,exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    abstract val chatDatabaseDao: ChatDatabaseDao
    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        fun getInstance(context: Context): ChatDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ChatDatabase::class.java,
                        "chat_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}