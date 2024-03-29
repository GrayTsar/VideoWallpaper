package com.graytsar.livewallpaper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.gms.ads.MobileAds
import com.graytsar.livewallpaper.databinding.ActivityRaibuBinding

class ReibuActivity : AppCompatActivity() {
    private var _binding: ActivityRaibuBinding? = null
    private val binding: ActivityRaibuBinding
        get() = _binding!!
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityRaibuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.includeToolbar.toolbar
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        NavigationUI.setupActionBarWithNavController(this, navController)

        appBarConfiguration = AppBarConfiguration(setOf(R.id.fragmentMain))

        binding.includeToolbar.toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }

        try {
            MobileAds.initialize(this)
        } catch (e: Exception) {
            //do nothing
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
