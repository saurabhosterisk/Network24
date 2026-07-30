package com.network24.player.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.network24.player.databinding.ItemCategoryBinding
import com.network24.player.models.LiveCategory

class CategoryAdapter(
    private val list: MutableList<LiveCategory>,
    private val onClick: (LiveCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        holder.binding.txtCategory.text = item.category_name

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = list.size
}