package com.graytsar.livewallpaper

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.graytsar.livewallpaper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mask = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        when (mask){
            Configuration.UI_MODE_NIGHT_YES -> {
                SingletonStatic.isNightMode = true

                val edit = sharedPref.edit()
                edit.putBoolean(getString(R.string.keyThemeDarkMode), true)
                edit.apply()
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                val c = sharedPref.getBoolean(getString(R.string.keyThemeDarkMode), valueDefaultDarkMode)
                if(sharedPref.getBoolean(getString(R.string.keyThemeDarkMode), valueDefaultDarkMode)){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    SingletonStatic.isNightMode = true
                } else {
                    SingletonStatic.isNightMode = false

                    val edit = sharedPref.edit()
                    edit.putBoolean(getString(R.string.keyThemeDarkMode), false)
                    edit.apply()
                }
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                SingletonStatic.isNightMode = false

                val edit = sharedPref.edit()
                edit.putBoolean(getString(R.string.keyThemeDarkMode), false)
                edit.apply()
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.includeToolbar.toolbar
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        NavigationUI.setupActionBarWithNavController(this, navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragmentMain
            )
        )
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val f = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = f.navController //for fragment switch

        when(item.itemId) {
            R.id.menuSettings -> {
                if(f.childFragmentManager.fragments[0] is FragmentContainerSettings) {
                    //do nothing
                    //settings is already open
                } else {
                    navController.navigate(R.id.fragmentContainerSettings)
                }

            } else -> {
                navController.popBackStack()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
