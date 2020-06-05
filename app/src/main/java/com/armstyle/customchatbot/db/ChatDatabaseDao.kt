package com.armstyle.customchatbot.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.armstyle.customchatbot.vo.ChatMessage

@Dao
interface ChatDatabaseDao {
    @Insert
    fun insert(night: ChatMessage)

    @Update
    fun update(night: ChatMessage)

    @Query("SELECT * from chat_history_table WHERE id = :key ")
    fun get(key:Long):ChatMessage?

    @Query("DELETE FROM chat_history_table")
    fun clear()

    @Query("SELECT * FROM chat_history_table ORDER BY id ASC")
    fun getAllChat(): LiveData<List<ChatMessage>>

    @Query("SELECT * FROM chat_history_table where id >= :key ORDER BY id ASC")
    fun getAllChat10(key:Long): LiveData<List<ChatMessage>>

    @Query("SELECT * FROM chat_history_table where id < :key ORDER BY id DESC limit :limit")
    fun getAllChatLoadMore(key:Long, limit:Int): List<ChatMessage>

    @Query("SELECT * from chat_history_table order by id desc limit :limit")
    fun getLastLoadId(limit:Int): LiveData<List<ChatMessage>>

    @Query("SELECT * from chat_history_table order by id desc limit :limit")
    fun getLastLoad10(limit:Int): List<ChatMessage>
}