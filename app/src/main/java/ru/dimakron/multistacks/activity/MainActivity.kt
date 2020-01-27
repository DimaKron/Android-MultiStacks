package ru.dimakron.multistacks.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import ru.dimakron.multistacks.R
import ru.dimakron.multistacks.fragment.FavouritesFragment
import ru.dimakron.multistacks.fragment.HomeFragment
import ru.dimakron.multistacks.fragment.NewsFragment
import ru.dimakron.multistacks.fragment.ProfileFragment
import ru.dimakron.multistacks.model.NavigationTab
import ru.dimakron.multistacks_lib.BackResultType
import ru.dimakron.multistacks_lib.MultiStacks

class MainActivity : AppCompatActivity(),
    IMainActivity, MultiStacks.TransactionListener {

    private val tabs = listOf(
        NavigationTab(R.id.item_home) { HomeFragment.newInstance() },
        NavigationTab(R.id.item_news) { NewsFragment.newInstance() },
        NavigationTab(R.id.item_favourites) { FavouritesFragment.newInstance() },
        NavigationTab(R.id.item_profile) { ProfileFragment.newInstance() }
    )

    private lateinit var multiStacks: MultiStacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        multiStacks = MultiStacks.Builder(supportFragmentManager, R.id.containerLayout)
            .setRootFragmentInitializers(tabs.map { it.fragmentInitializer })
            .setSelectedTabIndex(0)
            .setTabHistoryEnabled(true)
            .setTransactionListener(this)
            .build()

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected)
    }

    override fun onDestroy() {
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        val result = multiStacks.onBackPressed()
        if (result.type == BackResultType.CANCELLED){
            super.onBackPressed()
        } else {
            result.newIndex?.let { bottomNavigationView.selectedItemId = tabs[it].tabId }
        }
    }

    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        // ...
    }

    override fun onFragmentTransaction(fragment: Fragment?) {
        // ...
    }

    override fun pushFragment(fragment: Fragment) {
        multiStacks.push(fragment)
    }

    override fun switchToHome() {
        bottomNavigationView.selectedItemId = R.id.item_home
    }

    override fun clearStack() {
        multiStacks.clearStack()
    }

    override fun replaceWithProfile() {
        multiStacks.replace(ProfileFragment.newInstance())
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        val newPosition = tabs.indexOfFirst { it.tabId == item.itemId }

        if (newPosition == multiStacks.getSelectedTabIndex()) {
            multiStacks.clearStack()
        } else {
            multiStacks.setSelectedTabIndex(newPosition)
        }

        return true
    }


}
