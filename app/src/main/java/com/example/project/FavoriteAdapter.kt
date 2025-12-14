package com.example.project

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.model.Quote

class FavoriteAdapter(
    private val context: Context,
    private val items: MutableList<Quote>,
    private val onDelete: (Quote) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.item_quote_text)
        val author: TextView = view.findViewById(R.id.item_quote_author)
        val share: ImageButton = view.findViewById(R.id.item_share)
        val delete: ImageButton = view.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = items[position]
        holder.text.text = q.text
        holder.author.text = q.author?.let { "— $it" } ?: ""
        holder.share.setOnClickListener {
            val send = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "\"${q.text}\" — ${q.author ?: "Unknown"}")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(send, "Поделиться цитатой"))
        }
        holder.delete.setOnClickListener {
            val qdel = items[position]
            items.removeAt(position)
            notifyItemRemoved(position)
            onDelete(qdel)
        }
    }

    override fun getItemCount(): Int = items.size

    fun add(quote: Quote) {
        items.add(0, quote)
        notifyItemInserted(0)
    }
}
