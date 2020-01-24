package ru.dimakron.multistacks_lib

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.json.JSONArray
import kotlin.math.min

class MultiStacks private constructor(builder: Builder) {

    private val containerId = builder.containerId
    private val fragmentManager = builder.fragmentManager
    private val rootFragmentInitializers = builder.rootFragmentInitializers
    private val transactionListener = builder.transactionListener
    private val isTabHistoryEnabled = builder.isTabHistoryEnabled

    private val fragmentStacks = mutableListOf<MutableList<Fragment>>()
    private val fragmentStates = mutableMapOf<Pair<Int, Int>, Fragment.SavedState?>()
    private var selectedTabIndex = -1
    private var isTransactionExecuting = false
    private var tabsHistory = UniqueStack()

    init {
        if (!restoreInstanceState(builder.savedInstanceState)) {
            repeat(rootFragmentInitializers.size) { fragmentStacks.add(mutableListOf()) }
            setSelectedTabIndex(builder.selectedTabIndex)
        }
    }

    private fun detachFragment(transaction: FragmentTransaction){
        val oldStack = fragmentStacks.getOrNull(selectedTabIndex)?: return
        val oldFragment = oldStack.lastOrNull()
        if(oldFragment != null){
            fragmentStates[Pair(selectedTabIndex, oldStack.lastIndex)] = fragmentManager.saveFragmentInstanceState(oldFragment)
            transaction.remove(oldFragment)
        }
    }

    private fun removeFragment(transaction: FragmentTransaction){
        val currentStack = fragmentStacks.getOrNull(selectedTabIndex)?: return
        val fragmentToRemove = currentStack.lastOrNull()
        if(fragmentToRemove != null){
            fragmentStates[Pair(selectedTabIndex, currentStack.lastIndex)] = null
            currentStack.remove(fragmentToRemove)
            transaction.remove(fragmentToRemove)
        }
    }

    private fun attachFragment(transaction: FragmentTransaction){
        val newStack = fragmentStacks.getOrNull(selectedTabIndex)?: return
        var newFragment = newStack.lastOrNull()
        if (newFragment == null){
            newFragment = rootFragmentInitializers[selectedTabIndex].invoke()
            newStack.add(newFragment)
        }
        attachFragment(transaction, newFragment)
    }

    private fun attachFragment(transaction: FragmentTransaction, fragment: Fragment){
        val newStack = fragmentStacks.getOrNull(selectedTabIndex)?: return
        fragment.setInitialSavedState(fragmentStates[Pair(selectedTabIndex, newStack.lastIndex)])
        transaction.add(containerId, fragment)
    }

    fun setSelectedTabIndex(index: Int){
        require(index >= 0 && index < fragmentStacks.size) { "Tab index should be in range [0, ${fragmentStacks.size - 1} but is $index]" }

        if (selectedTabIndex == index) return

        val transaction = fragmentManager.beginTransaction()

        detachFragment(transaction)

        selectedTabIndex = index

        attachFragment(transaction)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onTabTransaction(fragmentStacks[selectedTabIndex].lastOrNull(), selectedTabIndex)
        tabsHistory.push(selectedTabIndex)
    }

    fun getSelectedTabIndex() = selectedTabIndex

    fun push(fragment: Fragment) {
        val currentStack = fragmentStacks[selectedTabIndex]

        val transaction = fragmentManager.beginTransaction()

        detachFragment(transaction)

        /*if ((fragment as? IMultiStackFragment)?.getIdentifierInStack() != null) TODO
            currentStack.filter { (it as? IMultiStackFragment)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                .forEach { f ->
                    currentStack.remove(f)
                    fragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                }*/

        currentStack.add(fragment)
        attachFragment(transaction, fragment)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onFragmentTransaction(currentStack.lastOrNull())
    }

    fun popFragments(depth: Int) {
        require(depth > 0) { "Pop depth should be greater than 0" }

        val currentStack = fragmentStacks[selectedTabIndex]

        val transaction = fragmentManager.beginTransaction()

        val removesCount = min(depth, currentStack.size - 1)
        for (i in 0 until removesCount) removeFragment(transaction)

        if (removesCount > 0) attachFragment(transaction)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onFragmentTransaction(currentStack.lastOrNull())
    }

    fun clearStack() {
        fragmentStacks[selectedTabIndex].size.takeIf { it > 0 }?.let { popFragments(it) }
    }

    fun replace(fragment: Fragment) {
        val currentStack = fragmentStacks[selectedTabIndex]

        val transaction = fragmentManager.beginTransaction()

        removeFragment(transaction)

        /*if ((fragment as? IMultiStackFragment)?.getIdentifierInStack() != null) TODO
            currentStack.filter { (it as? IMultiStackFragment)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        fragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }*/

        currentStack.add(fragment)
        attachFragment(transaction, fragment)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onFragmentTransaction(currentStack.lastOrNull())
    }

    fun isRootFragment() = fragmentStacks[selectedTabIndex].size == 1

    fun onBackPressed(): BackResult{
        if (isRootFragment()) {
            return when {
                tabsHistory.isEmpty() || tabsHistory.getSize() == 1 && tabsHistory.peek() == 0 || !isTabHistoryEnabled  -> BackResult(BackResultType.CANCELLED)
                tabsHistory.getSize() > 1 -> {
                    tabsHistory.pop()
                    BackResult(BackResultType.OK, tabsHistory.peek())
                }
                else -> {
                    tabsHistory.clear()
                    BackResult(BackResultType.OK, 0)
                }
            }
        } else {
            popFragments(1)
            return BackResult(BackResultType.OK)
        }
    }

    private fun executePendingTransactions() {
        if (!isTransactionExecuting) {
            isTransactionExecuting = true
            fragmentManager.executePendingTransactions()
            isTransactionExecuting = false
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(Constants.Extras.TAB_HISTORY, tabsHistory)

        try {
            val stackArrays = JSONArray()
            fragmentStacks.forEach { stack ->
                val stackArray = JSONArray()
                stack.forEach { stackArray.put(it.tag) }
                stackArrays.put(stackArray)
            }
            outState.putString(Constants.Extras.FRAGMENT_STACKS, stackArrays.toString())
        } catch (t: Throwable) {
        }

        outState.putInt(Constants.Extras.SELECTED_TAB_INDEX, selectedTabIndex)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState == null) return false

        tabsHistory = savedInstanceState.getParcelable(Constants.Extras.TAB_HISTORY)?: UniqueStack()

        try {
            val stackArrays = JSONArray(savedInstanceState.getString(Constants.Extras.FRAGMENT_STACKS))

            for (x in 0 until stackArrays.length()) {
                val stackArray = stackArrays.getJSONArray(x)
                val stack = mutableListOf<Fragment>()
                if (stackArray.length() == 1) {
                    val fragment = stackArray.getString(0)?.takeUnless { it.equals("null", true) }
                        ?.let { fragmentManager.findFragmentByTag(it) }?: rootFragmentInitializers.getOrNull(x)?.invoke()
                    fragment?.let { stack.add(it) }
                } else {
                    for (y in 0 until stackArray.length()) {
                        val fragment = stackArray.getString(y)?.takeUnless { it.equals("null", true) }?.let { fragmentManager.findFragmentByTag(it) }
                        fragment?.let { stack.add(it) }
                    }
                }
                fragmentStacks.add(stack)
            }
        } catch (t: Throwable) {
            return false
        }

        setSelectedTabIndex(savedInstanceState.getInt(Constants.Extras.SELECTED_TAB_INDEX))

        return true
    }

    interface TransactionListener {
        fun onTabTransaction(fragment: Fragment?, index: Int)
        fun onFragmentTransaction(fragment: Fragment?)
    }

    class Builder(val fragmentManager: FragmentManager,
                  @IdRes val containerId: Int) {

        var savedInstanceState: Bundle? = null
        val rootFragmentInitializers = mutableListOf<() -> Fragment>()
        var selectedTabIndex = 0
        var transactionListener: TransactionListener? = null
        var isTabHistoryEnabled = false

        fun setState(state: Bundle?) = apply { savedInstanceState = state }

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionListener(listener: TransactionListener) = apply { transactionListener = listener }

        fun setTabHistoryEnabled(isEnabled: Boolean) = apply { isTabHistoryEnabled = isEnabled }

        fun build() = MultiStacks(this)
    }
}
