package com.network24.player.features.live.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.NavigationMenuView
import com.network24.player.R
import com.network24.player.features.live.adapter.CategoryAdapter
import com.network24.player.features.live.adapter.FavouriteCategoryAdapter
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityLiveCategoryBinding
import com.network24.player.features.dashboard.activity.DashboardActivity
import com.network24.player.features.live.models.LiveCategory
import com.network24.player.features.login.activity.LoginActivity
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.live.repository.SyncCallback
import kotlinx.coroutines.launch
import kotlin.math.abs

class LiveCategoryActivity : BaseActivity() {
    private lateinit var binding: ActivityLiveCategoryBinding
    private lateinit var repository: LiveRepository
    private lateinit var prefs: PreferenceManager

    private val allCategories = mutableListOf<LiveCategory>()
    private val favouriteCategories = mutableListOf<LiveCategory>()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var favouriteAdapter: FavouriteCategoryAdapter

    // 🔥 Loading Dialog Variable
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository(CacheManager(this))

        // ===== Right drawer menu wiring (3 dots) =====
        binding.btnMore.setOnClickListener {
            openRightDrawer(binding.drawerLayout)
        }

        setupOptionalRightDrawerMenu(
            drawerLayout = binding.drawerLayout,
            navView = binding.rightNav
        ) { itemId ->
            when (itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.action_refresh_all -> {
                    // 🔥 Naya force refresh call yahan add kiya
                    forceRefreshData()
                    true
                }
                R.id.action_refresh_guide -> {
                    Toast.makeText(this, "Refreshing TV Guide...", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_settings -> {
                    // If you have a SettingsActivity, put it here:
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    prefs.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                    true
                }
                else -> false
            }
        }

        // Focus first item when the right drawer opens (DPAD friendly)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                if (drawerView.id == binding.rightNav.id) {
                    binding.rightNav.post {
                        val menuView = binding.rightNav.getChildAt(0) as? NavigationMenuView
                        // Try to focus the first focusable row
                        if (menuView != null) {
                            for (i in 0 until menuView.childCount) {
                                val child = menuView.getChildAt(i)
                                if (child.isFocusable) {
                                    child.requestFocus()
                                    break
                                }
                            }
                        }
                    }
                }
            }
        })

        // ============================================
        setupRecyclerViews()
        setupSearch()

        // First load (will use cache if available)
        loadCategories(forceRefresh = false)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    // ============================================
    // 🔥 LOADER DIALOG FUNCTIONS
    // ============================================
    private fun showLoader() {
        if (loadingDialog == null) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(view)
            builder.setCancelable(false)
            loadingDialog = builder.create()
            loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        if (!isFinishing && loadingDialog?.isShowing == false) {
            loadingDialog?.show()
        }
    }

    private fun hideLoader() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog?.dismiss()
        }
    }

    // ============================================
    // 🔥 MANUAL REFRESH FUNCTION
    // ============================================
    private fun forceRefreshData() {
        showLoader()

        repository.syncAllData(
            server = prefs.getServer(),
            username = prefs.getUsername(),
            password = prefs.getPassword(),
            callback = object : SyncCallback {
                override fun onSuccess() {
                    hideLoader()
                    prefs.setLastSyncTime(System.currentTimeMillis())
                    Toast.makeText(this@LiveCategoryActivity, "Channels Updated Successfully!!", Toast.LENGTH_SHORT).show()

                    // Reload data in UI bypassing the cache this time
                    loadCategories(forceRefresh = true)
                }

                override fun onError(message: String) {
                    hideLoader()
                    Toast.makeText(this@LiveCategoryActivity, "Failed to refresh: $message", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun setupRecyclerViews() {
        val columns = if (resources.configuration.smallestScreenWidthDp >= 600) 6 else 3
        // Categories - Grid Layout
        binding.rvCategories.layoutManager = GridLayoutManager(this, columns)

        // Favourites - Horizontal Scroll
        binding.rvFavourite.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // --- Alignment snap helper (start aligned) ---
        val snapHelper = object : LinearSnapHelper() {
            override fun calculateDistanceToFinalSnap(
                layoutManager: RecyclerView.LayoutManager,
                targetView: View
            ): IntArray {
                val out = IntArray(2)
                val viewStart = targetView.left - layoutManager.getLeftDecorationWidth(targetView)
                out[0] = viewStart - layoutManager.paddingLeft
                out[1] = 0
                return out
            }
            override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
                if (layoutManager !is LinearLayoutManager) return null
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val firstView = layoutManager.findViewByPosition(0)
                // Avoid jitter when returning to start
                if (firstVisible == 0 && firstView != null) {
                    val viewStart = firstView.left - layoutManager.getLeftDecorationWidth(firstView)
                    val distance = abs(viewStart - layoutManager.paddingLeft)
                    if (distance < firstView.width / 2) {
                        return null
                    }
                }
                var closestChild: View? = null
                var closestDistance = Int.MAX_VALUE
                for (i in 0 until layoutManager.childCount) {
                    val child = layoutManager.getChildAt(i) ?: continue
                    val viewStart = child.left - layoutManager.getLeftDecorationWidth(child)
                    val distance = abs(viewStart - layoutManager.paddingLeft)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestChild = child
                    }
                }
                return closestChild
            }
        }
        snapHelper.attachToRecyclerView(binding.rvFavourite)
        // --------------------------------------------

        categoryAdapter = CategoryAdapter(
            listener = { openCategory(it) },
            onLongClick = { addToFavourites(it) }
        )
        favouriteAdapter = FavouriteCategoryAdapter(
            columns = columns,
            listener = { openCategory(it) },
            onLongClick = { removeFromFavourites(it) }
        )
        binding.rvCategories.adapter = categoryAdapter
        binding.rvFavourite.adapter = favouriteAdapter
        binding.rvCategories.setHasFixedSize(true)
        binding.rvFavourite.setHasFixedSize(true)

        Log.d("LIVE_CATEGORY", "Recycler initialized")
    }

    // 🔥 Added forceRefresh parameter to reload UI properly after sync
    fun loadCategories(forceRefresh: Boolean = false) {
        Log.d("LIVE_CATEGORY", "Loading categories...")
        // Search se default focus hata do
        binding.edtSearch.clearFocus()
        // Correct visibility immediately
        updateFavouritesSectionVisibility()

        lifecycleScope.launch {
            try {
                val categories = repository.getCategories(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    forceRefresh = forceRefresh
                )
                allCategories.clear()
                allCategories.addAll(categories)
                categoryAdapter.updateList(allCategories)

                val savedFavIds = getSavedFavourites()
                favouriteCategories.clear()
                for (category in allCategories) {
                    if (savedFavIds.contains(category.category_id.toString())) {
                        favouriteCategories.add(category)
                    }
                }
                binding.txtCategoryCount.text = "${allCategories.size} Categories"
                favouriteAdapter.updateList(favouriteCategories)

                updateFavouritesSectionVisibility()
                binding.rvCategories.post {
                    binding.rvCategories.adapter?.notifyDataSetChanged()
                    binding.rvCategories.postDelayed({
                        binding.rvCategories.layoutManager
                            ?.findViewByPosition(0)
                            ?.requestFocus()
                    }, 50)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LiveCategoryActivity,
                    e.message ?: "Unknown Error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filter(s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun filter(keyword: String) {
        val filtered = allCategories.filter {
            it.category_name.contains(keyword, true)
        }
        categoryAdapter.updateList(filtered)
    }

    private fun openCategory(category: LiveCategory) {
        val intent = Intent(this, ChannelListActivity::class.java)
        intent.putExtra("category_id", category.category_id)
        intent.putExtra("category_name", category.category_name)
        startActivity(intent)
    }

    // --- FAVOURITES SAVE & LOAD LOGIC ---
    private fun getSavedFavourites(): Set<String> {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getStringSet("fav_categories", emptySet()) ?: emptySet()
    }

    private fun saveFavouritesList(favIds: Set<String>) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("fav_categories", favIds).apply()
    }

    private fun addToFavourites(category: LiveCategory) {
        val currentFavs = getSavedFavourites().toMutableSet()
        val catId = category.category_id.toString()
        if (currentFavs.contains(catId)) {
            Toast.makeText(this, "${category.category_name} already in Favourites", Toast.LENGTH_SHORT).show()
            return
        }
        currentFavs.add(catId)
        saveFavouritesList(currentFavs)
        favouriteCategories.add(category)
        favouriteAdapter.updateList(favouriteCategories)
        updateFavouritesSectionVisibility()
        Toast.makeText(this, "${category.category_name} added to Favourites", Toast.LENGTH_SHORT).show()
    }

    private fun removeFromFavourites(category: LiveCategory) {
        val currentFavs = getSavedFavourites().toMutableSet()
        val catId = category.category_id.toString()
        currentFavs.remove(catId)
        saveFavouritesList(currentFavs)
        favouriteCategories.removeAll { it.category_id.toString() == catId }
        favouriteAdapter.updateList(favouriteCategories)
        updateFavouritesSectionVisibility()
        Toast.makeText(this, "${category.category_name} removed from Favourites", Toast.LENGTH_SHORT).show()
    }

    private fun updateFavouritesSectionVisibility() {
        val hasFav = favouriteCategories.isNotEmpty()
        binding.favouritesSection.visibility = if (hasFav) View.VISIBLE else View.GONE
        binding.txtFavouriteCount.text = "${favouriteCategories.size} Favourites"
    }

    override fun onDestroy() {
        hideLoader() // Leak prevent karne ke liye
        super.onDestroy()
    }
}