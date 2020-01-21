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
import ru.dimakron.multistacks_lib.MultiStacks

class MainActivity : AppCompatActivity(),
    IMainActivity {

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
            .setState(savedInstanceState)
            .setRootFragmentInitializers(tabs.map { it.fragmentInitializer })
            .setSelectedTabIndex(0)
            .build()

        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        multiStacks.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        bottomNavigationView.setOnNavigationItemSelectedListener(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (multiStacks.isRootFragment()) {
            /*val history = tabsHistory ?: return
            when {
                history.isEmpty() -> showExitDialog()
                history.getSize() > 1 -> {
                    history.pop()
                    bottomMenu?.selectedItemId = navigationTabs[history.peek()!!].tabId
                }
                else -> {
                    bottomMenu?.selectedItemId = navigationTabs[0].tabId
                    history.clear()
                }
            }*/
            super.onBackPressed()
        } else {
            multiStacks.popFragments(1)
        }
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
            //tabsHistory?.push(newPosition)
        }

        return true
    }
}
