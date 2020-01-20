package ru.dimakron.multistacks

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import ru.dimakron.multistacks_lib.IMultiStackActivity
import ru.dimakron.multistacks_lib.MultiStacks

class MainActivity : AppCompatActivity(), IMultiStackActivity {

    private val tabs = listOf(
        NavigationTab(R.id.item_home) { SimpleFragment.newInstance(getString(R.string.main_item_home)) },
        NavigationTab(R.id.item_news) { SimpleFragment.newInstance(getString(R.string.main_item_news)) },
        NavigationTab(R.id.item_favourite) { SimpleFragment.newInstance(getString(R.string.main_item_favourite)) },
        NavigationTab(R.id.item_profile) { SimpleFragment.newInstance(getString(R.string.main_item_profile)) }
    )

    private lateinit var multiStacks: MultiStacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        multiStacks = MultiStacks.Builder(supportFragmentManager, R.id.containerLayout)
            .setState(savedInstanceState)
            .setRootFragmentInitializers(tabs.map { it.fragmentInitializer })
            .setSelectedTabIndex(0)
            .build()

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected)
    }

    override fun onDestroy() {
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        super.onDestroy()
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        val newPosition = tabs.indexOfFirst { it.tabId == item.itemId }

        if (newPosition == multiStacks.getSelectedTabIndex()){
            multiStacks.clearStack()
        } else {
            multiStacks.setSelectedTabIndex(newPosition)
            //tabsHistory?.push(newPosition)
        }

        return true
    }

    override fun pushFragment(fragment: Fragment) {
        multiStacks.push(fragment)
    }
}
