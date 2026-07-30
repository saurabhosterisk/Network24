package com.network24.player.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.network24.player.R
import com.network24.player.databinding.ItemChannelBinding
import com.network24.player.models.LiveChannel

class ChannelAdapter(
    private val list: MutableList<LiveChannel>,
    private val onClick: (LiveChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val channel = list[position]

        holder.binding.txtChannel.text = channel.name ?: ""

        holder.binding.imgLogo.load(channel.stream_icon) {

            crossfade(true)

            placeholder(R.drawable.ic_launcher_foreground)

            error(R.drawable.ic_launcher_foreground)

        }

        holder.itemView.setOnClickListener {

            onClick(channel)

        }

    }

    override fun getItemCount(): Int = list.size

}