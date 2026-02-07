package com.softcraft.dolphin.ui

import android.os.Bundle
import android.view.KeyEvent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.softcraft.dolphin.R
import com.softcraft.dolphin.data.repository.ChatRepository
import com.softcraft.dolphin.data.repository.ConfigRepository
import com.softcraft.dolphin.utils.GeminiHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var tvConfigStatus: TextView
    private lateinit var spinnerModel: Spinner

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var configRepository: ConfigRepository
    private lateinit var chatRepository: ChatRepository

    private var currentConfig: com.softcraft.dolphin.data.model.AiConfig? = null
    private var isSpinnerInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        initializeRepositories()
        setupListeners()
        observeConfig()
        observeMessages()
    }

    private fun initializeViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvConfigStatus = findViewById(R.id.tvConfigStatus)
        spinnerModel = findViewById(R.id.spinnerModel)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = chatAdapter
    }

    private fun initializeRepositories() {
        val firestore = FirebaseFirestore.getInstance()
        val geminiHelper = GeminiHelper()

        configRepository = ConfigRepository(firestore)
        chatRepository = ChatRepository(geminiHelper)
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            sendMessage()
        }

        etMessage.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                sendMessage()
                return@setOnKeyListener true
            }
            false
        }

        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) return

                val config = currentConfig ?: return
                val selectedModelName = parent?.getItemAtPosition(position) as? String ?: return
                val selectedModelValue = config.availableModels[selectedModelName]

                if (selectedModelValue != null && selectedModelValue != config.modelName) {
                    lifecycleScope.launch {
                        val success = configRepository.updateModel(selectedModelValue)
                        if (success) {
                            Toast.makeText(
                                this@MainActivity,
                                "Model changed to: $selectedModelName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeConfig() {
        lifecycleScope.launch {
            configRepository.getAiConfigStream().collectLatest { config ->
                currentConfig = config
                updateConfigStatus(config)
                setupModelSpinner(config)
                // Show status in toolbar subtitle
                findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).subtitle =
                    if (config.isActive) "${config.modelName} | Ready" else "Inactive"
            }
        }
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            chatRepository.messages.collectLatest { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun setupModelSpinner(config: com.softcraft.dolphin.data.model.AiConfig) {
        val availableModels = config.availableModels
        if (availableModels.isEmpty()) return

        val modelNames = availableModels.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModel.adapter = adapter

        val currentDisplayName = config.getCurrentModelDisplayName()
        val currentIndex = modelNames.indexOfFirst { it == currentDisplayName }

        if (currentIndex >= 0) {
            isSpinnerInitialized = false
            spinnerModel.setSelection(currentIndex)
            spinnerModel.post {
                isSpinnerInitialized = true
            }
        } else {
            isSpinnerInitialized = true
        }
    }

    private fun updateConfigStatus(config: com.softcraft.dolphin.data.model.AiConfig) {
        val currentModelDisplay = config.getCurrentModelDisplayName()
        tvConfigStatus.text = if (config.isActive) {
            "Connected to $currentModelDisplay"
        } else {
            "AI service is inactive"
        }

        btnSend.isEnabled = config.isActive && config.apiKey.isNotBlank()
    }

    private fun sendMessage() {
        val message = etMessage.text?.toString()?.trim()
        if (message.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            val config = currentConfig ?: run {
                Toast.makeText(this@MainActivity, "Configuration not loaded", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (!config.isActive) {
                Toast.makeText(this@MainActivity, "AI service is inactive", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (config.apiKey.isBlank()) {
                Toast.makeText(this@MainActivity, "API key not configured", Toast.LENGTH_SHORT).show()
                return@launch
            }

            chatRepository.sendMessage(message, config)
            etMessage.text?.clear()
        }
    }
}