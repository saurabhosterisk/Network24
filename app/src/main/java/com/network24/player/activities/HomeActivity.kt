package com.network24.player.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.network24.player.adapters.CategoryAdapter
import com.network24.player.adapters.ChannelAdapter
import com.network24.player.databinding.ActivityHomeBinding
import com.network24.player.models.LiveCategory
import com.network24.player.models.LiveChannel
import com.network24.player.repository.LiveRepository
import com.network24.player.utils.PreferenceManager
import kotlinx.coroutines.launch
import android.content.Intent

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var prefs: PreferenceManager
    private lateinit var repository: LiveRepository

    private val categoryList = mutableListOf<LiveCategory>()
    private val channelList = mutableListOf<LiveChannel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository()

        setupCategoryRecycler()
        setupChannelRecycler()

        loadCategories()
    }

    private fun setupCategoryRecycler() {

        binding.rvCategories.layoutManager =
            LinearLayoutManager(this)

        binding.rvCategories.adapter =
            CategoryAdapter(categoryList) { category ->

                loadChannels(category.category_id)

            }
    }

    private fun setupChannelRecycler() {

        binding.rvChannels.layoutManager =
            LinearLayoutManager(this)

        binding.rvChannels.adapter =
            ChannelAdapter(channelList) { channel ->

                val intent = Intent(this, PlayerActivity::class.java)

                intent.putExtra("stream_id", channel.stream_id ?: 0)

                intent.putExtra("stream_name", channel.name)

                startActivity(intent)

            }
    }

    private fun loadCategories() {

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {

            try {

                val response = repository.getCategories(
                    prefs.getServer(),
                    prefs.getUsername(),
                    prefs.getPassword()
                )

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {

                    categoryList.clear()
                    categoryList.addAll(response.body()!!)
                    binding.rvCategories.adapter?.notifyDataSetChanged()

                    if (categoryList.isNotEmpty()) {

                        loadChannels(categoryList.first().category_id)

                    }

                } else {

                    Toast.makeText(
                        this@HomeActivity,
                        "Unable to load categories",
                        Toast.LENGTH_LONG
                    ).show()

                }

            } catch (e: Exception) {

                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    this@HomeActivity,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

        }

    }

    private fun loadChannels(categoryId: String?) {

        if (categoryId == null) return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {

            try {

                val response = repository.getChannels(
                    prefs.getServer(),
                    prefs.getUsername(),
                    prefs.getPassword(),
                    categoryId
                )

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {

                    channelList.clear()
                    channelList.addAll(response.body()!!)
                    binding.rvChannels.adapter?.notifyDataSetChanged()

                } else {

                    Toast.makeText(
                        this@HomeActivity,
                        "Unable to load channels",
                        Toast.LENGTH_LONG
                    ).show()

                }

            } catch (e: Exception) {

                binding.progressBar.visibility = View.GONE

                Toast.makeText(
                    this@HomeActivity,
                    e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()

            }

        }

    }

}