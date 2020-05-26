package com.armstyle.customchatbot


import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.AIServiceException
import ai.api.android.AIService
import ai.api.model.AIRequest
import ai.api.model.AIResponse
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.armstyle.customchatbot.adapter.MyAdapter
import com.armstyle.customchatbot.vo.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity()  {

    lateinit var aiService: AIService
    lateinit var aiDataAIService: AIDataService
    var user: String? = null

    private lateinit var adapter: MyAdapter

    val mRootRef = FirebaseDatabase.getInstance().reference
    val mUsersRef = mRootRef.child("users")
    val mMessagesRef = mRootRef.child("messages")

    val menuChatHistory: MutableList<ChatMessage> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voiceButton.setOnClickListener{
            val intent = Intent(this,VoiceActivity::class.java)
            startActivity(intent)
        }
//        mUsersRef.child("id-12345").setValue("Jirawatee")
//        val friendlyMessage = ChatMessage("Hello World!", "Jirawatee")
//        mMessagesRef.push().setValue(friendlyMessage)
//        mMessagesRef.push().setValue(ChatMessage("Hello", "Jirawatee"))

//        mMessagesRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                val value = dataSnapshot.value.toString()
//                Toast.makeText(this@MainActivity, value.toString(), Toast.LENGTH_SHORT).show()
//                Log.d("TAG", value.toString())
//
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(this@MainActivity, "Failed: ${error.message}" , Toast.LENGTH_SHORT).show()
//            }
//        })

        initOldChat()

        rvChat.layoutManager = LinearLayoutManager(this)
        adapter = MyAdapter(this)
        rvChat.adapter = adapter

        val config = ai.api.android.AIConfiguration(
            "5435097a2625406cb27b78b322f1088e",
            AIConfiguration.SupportedLanguages.English,
            ai.api.android.AIConfiguration.RecognitionEngine.System
        )

        val aiDataService = AIDataService(config)

        val aiRequest = AIRequest()

        btnSend.setOnClickListener {
            val message = edChat.text.toString()
            if (message != "") {
                val chatMessage = ChatMessage(message, "user")
                addMessageToChat(chatMessage)
                aiRequest.setQuery(message)
                mMessagesRef.push().setValue(chatMessage) // เพิ่มลง database
                object : AsyncTask<AIRequest?, Void?, AIResponse?>() {
                    override fun onPostExecute(aiResponse: AIResponse?) {
                        if (aiResponse != null) {
                            val chatMessage = ChatMessage(aiResponse.result.fulfillment.speech, "bot")

                            addMessageToChat(chatMessage)

                            mMessagesRef.push().setValue(chatMessage) // เพิ่มลง database

                            // Toast.makeText(this@MainActivity, aiResponse.result.fulfillment.speech, Toast.LENGTH_SHORT).show()
                            Log.d("Chat",aiResponse.result.resolvedQuery)
                            Log.d("Chat",aiResponse.result.action)
                            Log.d("Chat",aiResponse.result.fulfillment.speech)
                        }
                    }

                    override fun doInBackground(vararg p0: AIRequest?): AIResponse? {
                        val request = p0[0]
                        try {
                            return aiDataService.request(aiRequest)
                        } catch (e: AIServiceException) {
                        }
                        return null
                    }
                }.execute(aiRequest)

            } else {
                Toast.makeText(applicationContext, "Enter message first", Toast.LENGTH_SHORT).show()
            }
            edChat.setText("")

        }

    }

    private fun initOldChat(){
        val chatListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                menuChatHistory.clear()
                dataSnapshot.children.mapNotNullTo(menuChatHistory) { it.getValue(ChatMessage::class.java) }
                menuChatHistory.forEach { addMessageToChat(it) }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                println("loadPost:onCancelled ${databaseError.toException()}")
            }
        }
        mMessagesRef.addListenerForSingleValueEvent(chatListener)
    }

    private fun addMessageToChat(text: ChatMessage){
        runOnUiThread {
            adapter.addMessage(text)
            // scroll the RecyclerView to the last added element
            rvChat.scrollToPosition(adapter.itemCount - 1)
        }
    }

}
