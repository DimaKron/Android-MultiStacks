package ru.dimakron.multistacks_lib

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlin.math.min

class MultiStacks private constructor(builder: Builder) {

    companion object{
        private const val UNDEFINED_TAB_INDEX = -1
    }

    private val containerId = builder.containerId
    private val fragmentManager = builder.fragmentManager
    private val rootFragmentInitializers = builder.rootFragmentInitializers
    private val transactionListener = builder.transactionListener
    private val isTabHistoryEnabled = builder.isTabHistoryEnabled

    private var isTransactionExecuting = false

    private val fragmentStacks get() = State.fragmentStacks
    private val fragmentStates get() = State.fragmentStates
    private val tabsHistory get() = State.tabsHistory
    private var tabIndex
        get() = State.selectedTabIndex
        set(value) { State.selectedTabIndex = value }

    init {
        if (fragmentStacks.isEmpty()) {
            repeat(rootFragmentInitializers.size) { fragmentStacks.add(mutableListOf()) }
            setSelectedTabIndex(builder.selectedTabIndex)
        } else {
            val actualIndex = tabIndex // Такой маневр нужен для избежания проблем с detachFragment
            tabIndex = UNDEFINED_TAB_INDEX
            setSelectedTabIndex(actualIndex)
        }
    }

    fun setSelectedTabIndex(index: Int){
        require(index >= 0 && index < fragmentStacks.size) { "Tab index should be in range [0, ${fragmentStacks.size - 1} but is $index]" }

        if (tabIndex == index) return

        val transaction = fragmentManager.beginTransaction()

        detachFragment(transaction)

        tabIndex = index

        attachFragment(transaction)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onTabTransaction(fragmentStacks[tabIndex].lastOrNull(), tabIndex)
        tabsHistory.push(tabIndex)
    }

    fun getSelectedTabIndex() = tabIndex

    fun push(fragment: Fragment) {
        val currentStack = fragmentStacks[tabIndex]

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

        val currentStack = fragmentStacks[tabIndex]

        val transaction = fragmentManager.beginTransaction()

        val removesCount = min(depth, currentStack.size - 1)
        for (i in 0 until removesCount) removeFragment(transaction)

        if (removesCount > 0) attachFragment(transaction)

        transaction.commit()

        executePendingTransactions()

        transactionListener?.onFragmentTransaction(currentStack.lastOrNull())
    }

    fun clearStack() {
        fragmentStacks[tabIndex].size.takeIf { it > 0 }?.let { popFragments(it) }
    }

    fun replace(fragment: Fragment) {
        val currentStack = fragmentStacks[tabIndex]

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

    fun isRootFragment() = fragmentStacks[tabIndex].size == 1

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

    private fun detachFragment(transaction: FragmentTransaction){
        val oldStack = fragmentStacks.getOrNull(tabIndex)?: return
        val oldFragment = oldStack.lastOrNull()
        if(oldFragment != null){
            fragmentStates[Pair(tabIndex, oldStack.lastIndex)] = fragmentManager.saveFragmentInstanceState(oldFragment)
            transaction.remove(oldFragment)
        }
    }

    private fun removeFragment(transaction: FragmentTransaction){
        val currentStack = fragmentStacks.getOrNull(tabIndex)?: return
        val fragmentToRemove = currentStack.lastOrNull()
        if(fragmentToRemove != null){
            fragmentStates[Pair(tabIndex, currentStack.lastIndex)] = null
            currentStack.remove(fragmentToRemove)
            transaction.remove(fragmentToRemove)
        }
    }

    private fun attachFragment(transaction: FragmentTransaction){
        val newStack = fragmentStacks.getOrNull(tabIndex)?: return
        var newFragment = newStack.lastOrNull()
        if (newFragment == null){
            newFragment = rootFragmentInitializers[tabIndex].invoke()
            newStack.add(newFragment)
        }
        attachFragment(transaction, newFragment)
    }

    private fun attachFragment(transaction: FragmentTransaction, fragment: Fragment){
        val newStack = fragmentStacks.getOrNull(tabIndex)?: return
        fragment.setInitialSavedState(fragmentStates[Pair(tabIndex, newStack.lastIndex)])
        transaction.add(containerId, fragment)
    }

    private fun executePendingTransactions() {
        if (!isTransactionExecuting) {
            isTransactionExecuting = true
            fragmentManager.executePendingTransactions()
            isTransactionExecuting = false
        }
    }

    interface TransactionListener {
        fun onTabTransaction(fragment: Fragment?, index: Int)
        fun onFragmentTransaction(fragment: Fragment?)
    }

    private object State{

        val fragmentStacks = mutableListOf<MutableList<Fragment>>()
        val fragmentStates = mutableMapOf<Pair<Int, Int>, Fragment.SavedState?>()
        val tabsHistory = UniqueStack()
        var selectedTabIndex = UNDEFINED_TAB_INDEX

        fun clear(){
            fragmentStacks.clear()
            fragmentStates.clear()
            tabsHistory.clear()
            selectedTabIndex = UNDEFINED_TAB_INDEX
        }
    }

    class Builder(val fragmentManager: FragmentManager,
                  @IdRes val containerId: Int) {

        val rootFragmentInitializers = mutableListOf<() -> Fragment>()
        var selectedTabIndex = 0
        var transactionListener: TransactionListener? = null
        var isTabHistoryEnabled = false

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionListener(listener: TransactionListener) = apply { transactionListener = listener }

        fun setTabHistoryEnabled(isEnabled: Boolean) = apply { isTabHistoryEnabled = isEnabled }

        fun build() = MultiStacks(this)
    }
}
