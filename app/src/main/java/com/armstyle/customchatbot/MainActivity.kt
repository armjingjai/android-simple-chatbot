package com.armstyle.customchatbot


import ai.api.AIConfiguration
import ai.api.AIDataService
import ai.api.AIListener
import ai.api.android.AIService
import ai.api.model.AIError
import ai.api.model.AIRequest
import ai.api.model.AIResponse
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.armstyle.customchatbot.adapter.MyAdapter
import com.armstyle.customchatbot.db.ChatDatabase
import com.armstyle.customchatbot.vo.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() , AIListener {

    // pref
    lateinit var sharedpreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    // api ai
    lateinit var aiService: AIService

    // firebase database
    private val mRootRef = FirebaseDatabase.getInstance().reference
    private val mMessagesRef = mRootRef.child(FIREBASE_DB_CHILD)


    private lateinit var adapter: MyAdapter
    val menuChatHistory: MutableList<ChatMessage> = mutableListOf()

    // scroll point
    var pastVisiblesItems: Int = 0
    var visibleItemCount: Int = 0
    var totalItemCount: Int = 0

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appDatabase = ChatDatabase.getInstance(this)

        sharedpreferences = getSharedPreferences(CHAT_PREF, Context.MODE_PRIVATE)
        editor = sharedpreferences.edit()

//        initOldChat() // load chat history from firebase
        initOldChatRoom(appDatabase) // load chat history from rom database
        initScrollListener()


        val config = ai.api.android.AIConfiguration(
            CLIENT_ACCESS_TOKEN,
            AIConfiguration.SupportedLanguages.English,
            ai.api.android.AIConfiguration.RecognitionEngine.System
        )

        voiceButton.setOnClickListener{

            aiService = AIService.getService(this, config)
            aiService.setListener(this)

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                aiService.startListening()
            }
        }

        rvChat.layoutManager = LinearLayoutManager(this)
        adapter = MyAdapter(this)
        rvChat.adapter = adapter

        val aiDataService = AIDataService(config)

        val aiRequest = AIRequest()

        btnSend.setOnClickListener {
            val message = edChat.text.toString()
            if (message != "") {
                val chatMessage = ChatMessage(0, message, TypeUsers.USER.type)
                addMessageToChat(chatMessage)// เพิ่ม message ลง UI
                aiRequest.setQuery(message)
//                mMessagesRef.push().setValue(chatMessage) // เพิ่มลง database
                addChatToRoom(appDatabase, chatMessage) // เพิ่มลง database room
                AsyncTask.execute(kotlinx.coroutines.Runnable {
                    val aiResponse :AIResponse = aiDataService.request(aiRequest)
                    if (aiResponse != null) {
                        val chatMessage = ChatMessage(0,aiResponse.result.fulfillment.speech, TypeUsers.BOT.type)

                        addMessageToChat(chatMessage)// เพิ่ม message ลง UI
//                            mMessagesRef.push().setValue(chatMessage) // เพิ่มลง database
                        addChatToRoom(appDatabase, chatMessage) // เพิ่มลง database room
                    }
                })
            } else {
                Toast.makeText(applicationContext, "Enter message first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ///////////// ใช้ database Firebase realtime database
    private fun initOldChat(){
        val chatListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                menuChatHistory.clear()

                dataSnapshot.children.mapNotNullTo(menuChatHistory) {
                    it.getValue(ChatMessage::class.java) }
                menuChatHistory.forEach {
                    addMessageToChat(it) }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                println("loadPost:onCancelled ${databaseError.toException()}")
            }
        }
        mMessagesRef.limitToLast(LIMIT_LOAD).addListenerForSingleValueEvent(chatListener)
    }

    /////////// ใช้ database room
    private fun initOldChatRoom(appDatabase: ChatDatabase) {
        AsyncTask.execute(kotlinx.coroutines.Runnable {
            val data = appDatabase.chatDatabaseDao.getLastLoad10(LIMIT_LOAD)
            data.asReversed().forEach {
                addMessageToChat(ChatMessage(0,it.chat, it.user))
            }
            if (data.isNotEmpty()){
                editor.putLong(LAST_LOAD_ID, data[data.size-1].id)
                editor.apply()
            }
        })
    }

    // เพิ่มลง database room
    private fun addChatToRoom(appDatabase: ChatDatabase, chatMessage: ChatMessage) {
        val chatmessage2 = ChatMessage()
        chatmessage2.chat = chatMessage.chat
        chatmessage2.user = chatMessage.user
        AsyncTask.execute(kotlinx.coroutines.Runnable { appDatabase.chatDatabaseDao.insert(chatmessage2) })
    }

    private fun initScrollListener(){
        rvChat.addOnScrollListener(object :RecyclerView.OnScrollListener(){

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                visibleItemCount = linearLayoutManager.childCount
                totalItemCount = linearLayoutManager.itemCount
                pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition()

                if(linearLayoutManager.findViewByPosition(pastVisiblesItems)!!.top==0 && pastVisiblesItems==0){
                    loadMoreOldChatRoom() // load more chat history from  database
                }
            }
        })
    }

    private fun loadMoreOldChatRoom(){ // load more จาก room
        val appDatabase = ChatDatabase.getInstance(this)
        sharedpreferences = getSharedPreferences(CHAT_PREF, Context.MODE_PRIVATE)
        val editor = sharedpreferences.edit()

        AsyncTask.execute(kotlinx.coroutines.Runnable {
            val data = appDatabase.chatDatabaseDao.getAllChatLoadMore(sharedpreferences.getLong(LAST_LOAD_ID, 0), LIMIT_LOAD)
            data.forEach {
                addMessageHistoryToChat(ChatMessage(0,it.chat, it.user))
            }
            if (data.isNotEmpty()){
                editor.putLong(LAST_LOAD_ID, data[data.size-1].id)
                editor.apply()
            }
        })
    }

    private fun addMessageToChat(text: ChatMessage){
        runOnUiThread {
            adapter.addMessage(text, TypePosittion.BOTTOM.type)
            // scroll the RecyclerView to the last added element
            rvChat.scrollToPosition(adapter.itemCount - 1)
            edChat.setText("")
        }
    }
    private fun addMessageHistoryToChat(text: ChatMessage){
        runOnUiThread {
            adapter.addMessage(text, TypePosittion.TOP.type)
            adapter.notifyItemInserted(0)
        }
    }

    override fun onResult(result: AIResponse?) {
        val appDatabase = ChatDatabase.getInstance(this)

        // Show results in TextView.
        if (result != null) {
            // user text
            val chatMessageUser = ChatMessage(0,result.result.resolvedQuery, TypeUsers.USER.type)
            addMessageToChat(chatMessageUser) // เพิ่ม message ลง UI
//            mMessagesRef.push().setValue(chatMessageUser) // เพิ่มลง database
            addChatToRoom(appDatabase, chatMessageUser) // เพิ่มลง database room
            // bot text
            val chatMessageBot = ChatMessage(0,result.result.fulfillment.speech, TypeUsers.BOT.type)
            addMessageToChat(chatMessageBot) // เพิ่ม message ลง UI
//            mMessagesRef.push().setValue(chatMessageBot) // เพิ่มลง database
            addChatToRoom(appDatabase, chatMessageBot) // เพิ่มลง database room
        }
    }

    override fun onListeningStarted() {}

    override fun onAudioLevel(level: Float) {}

    override fun onError(error: AIError?) {
        Log.d(TAG_CHATBOT, "query error_result : ${error.toString()}")
    }

    override fun onListeningCanceled() {}

    override fun onListeningFinished() {}

    companion object {
        // database
        const val LAST_LOAD_ID = "LAST_LOAD_ID"
        const val CHAT_PREF = "CHAT_PREF"
        const val LIMIT_LOAD = 10

        // chatbot
        const val CLIENT_ACCESS_TOKEN = "15134f0f2c0f47b2bf75ca0f87bc18bc"

        // firebase
        const val FIREBASE_DB_CHILD = "messages"

        // TAG
        const val TAG_CHATBOT = "TAG_chat"
    }

}
