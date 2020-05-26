package com.armstyle.customchatbot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.armstyle.customchatbot.R
import com.armstyle.customchatbot.vo.ChatMessage
import kotlinx.android.synthetic.main.item_bot_chat.view.*
import kotlinx.android.synthetic.main.item_user_chat.view.*


class MyAdapter (val context: Context): RecyclerView.Adapter<MyAdapter.MessageViewHolder>() {

    private val TYPE_BOT = 1
    private val TYPE_USER = 2

    private val messages: ArrayList<ChatMessage> = ArrayList()

    fun addMessage(message: ChatMessage){
        messages.add(message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].user == "bot") {
            TYPE_BOT
        } else {
            TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            TYPE_BOT-> {
                BotMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.item_bot_chat, parent, false))
            }else -> {
                UserMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.item_user_chat, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        holder?.bind(message)
    }

    inner class BotMessageViewHolder (view: View) : MessageViewHolder(view) {
        private var messageText: TextView = view.botText

        override fun bind(message: ChatMessage) {
            messageText.text = message.chat
        }
    }

    inner class UserMessageViewHolder (view: View) : MessageViewHolder(view) {
        private var messageText: TextView = view.userText

        override fun bind(message: ChatMessage) {
            messageText.text = message.chat
        }
    }

    open class MessageViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        open fun bind(message:ChatMessage) {}
    }


}