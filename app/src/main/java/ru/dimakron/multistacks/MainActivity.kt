package ru.dimakron.multistacks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import ru.dimakron.multistacks_lib.MultiStacks

class MainActivity : AppCompatActivity() {

    private val tabs = listOf(
        NavigationTab(R.id.item_home) { SimpleFragment.newInstance(getString(R.string.main_item_home)) },
        NavigationTab(R.id.item_news) { SimpleFragment.newInstance(getString(R.string.main_item_news)) },
        NavigationTab(R.id.item_favourite) { SimpleFragment.newInstance(getString(R.string.main_item_favourite)) },
        NavigationTab(R.id.item_profile) { SimpleFragment.newInstance(getString(R.string.main_item_profile)) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MultiStacks.Builder(supportFragmentManager, R.id.containerLayout)
            .setState(savedInstanceState)
            .setRootFragmentInitializers(tabs.map { it.fragmentInitializer })
            .build()

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected)
    }

    override fun onDestroy() {
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        super.onDestroy()
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        /*val newPosition = navigationTabs.indexOfFirst { it.tabId == item.itemId }

        if (navController?.getCurrentStackIndex() == newPosition) {
            navController?.clearStack()
        } else {
            tabsHistory?.push(newPosition)
        }
        navController?.switchTab(newPosition)*/

        return true
    }
}
