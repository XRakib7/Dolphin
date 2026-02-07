package com.softcraft.dolphin.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.softcraft.dolphin.R
import com.softcraft.dolphin.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) {
            R.layout.item_message_user
        } else {
            R.layout.item_message_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) 0 else 1
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.content
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(message.timestamp)

            // Change text color for errors
            if (message.error) {
                tvMessage.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}