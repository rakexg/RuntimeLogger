package com.rakeshgurudu.android.runtimelogger.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rakeshgurudu.android.runtimelogger.R
import com.rakeshgurudu.android.runtimelogger.core.LogModel


class RVAdapter(
    private val onListItemClickListener: OnListItemClick
) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {
    var listData: ArrayList<LogModel> = ArrayList()

    fun addItems(newItems: List<LogModel>) {
        var itemSize = 0
        if (listData.isNotEmpty()) {
            itemSize = listData.size
        }
        listData.addAll(newItems)
        notifyItemRangeInserted(itemSize, itemSize + newItems.size)
    }

    fun removeAllItems() {
        var itemSize = 0
        if (listData.isNotEmpty()) {
            itemSize = listData.size
        }
        listData.clear()
        notifyItemRangeRemoved(0, itemSize)
    }

    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem: View = layoutInflater.inflate(R.layout.item_log, parent, false)
        return ViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listData[position]
        holder.fileName.text = item.fileName
        holder.fileSize.text = String.format("%.2f MB", item.fileSize)
        holder.share.setOnClickListener {
            onListItemClickListener.onItemClick(it, item, position)
        }
        holder.delete.setOnClickListener {
            onListItemClickListener.onItemClick(it, item, position)
        }
    }


    override fun getItemCount(): Int {
        return listData.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var share: ImageView = itemView.findViewById(R.id.share) as ImageView
        var delete: ImageView = itemView.findViewById(R.id.delete) as ImageView
        var fileName: TextView = itemView.findViewById(R.id.fileName)
        var fileSize: TextView = itemView.findViewById(R.id.fileSize)

    }

    interface OnListItemClick {
        fun onItemClick(view: View, item: Any, position: Int) {
        }
    }
}