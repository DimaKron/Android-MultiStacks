package ru.dimakron.multistacks_lib

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.json.JSONArray

class MultiStacks private constructor(builder: Builder) {

    private val containerId = builder.containerId
    private val fragmentManager = builder.fragmentManager
    private val transactionListener = builder.transactionListener

    private val fragmentStacks = mutableListOf<MutableList<Fragment>>()
    private var selectedTabIndex = builder.selectedTabIndex
    private var isTransactionExecuting = false
    private var mCurrentFragment: Fragment? = null // TODO Rename?
    private var tagCounter = 0

    init {
        if (!restoreInstanceState(builder.savedInstanceState, builder.rootFragmentInitializers)) {
            builder.rootFragmentInitializers.forEach { fragmentStacks.add(mutableListOf(it())) }

            val transaction = fragmentManager.beginTransaction()

            fragmentManager.fragments.forEach { transaction.remove(it) }

            val fragment = getRootFragment(selectedTabIndex)
            transaction.add(containerId, fragment, fragment.generateTag())
            transaction.commit()

            executePendingTransactions()

            mCurrentFragment = fragment
            transactionListener?.onTabTransaction(mCurrentFragment, selectedTabIndex)
        }
    }

    fun setSelectedTabIndex(index: Int){
        require(index >= 0 && index < fragmentStacks.size) { "Tab index should be in range [0, ${fragmentStacks.size - 1} but is $index]" }

        if (selectedTabIndex == index) return

        selectedTabIndex = index

        val transaction = fragmentManager.beginTransaction()

        getCurrentFragment()?.let { transaction.detach(it) }

        var fragment = reattachPreviousFragment(transaction)
        if (fragment == null) {
            fragment = getRootFragment(selectedTabIndex)
            transaction.add(containerId, fragment, fragment.generateTag())
        }
        transaction.commit()

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onTabTransaction(mCurrentFragment, selectedTabIndex)
    }

    fun getSelectedTabIndex() = selectedTabIndex

    fun push(fragment: Fragment) {
        val currentStack = fragmentStacks[selectedTabIndex]

        val transaction = fragmentManager.beginTransaction()

        getCurrentFragment()?.let { transaction.detach(it) }

        if ((fragment as? IMultiStackFragment)?.getIdentifierInStack() != null)
            currentStack.filter { (it as? IMultiStackFragment)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                .forEach { f ->
                    currentStack.remove(f)
                    fragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                }

        transaction.add(containerId, fragment, fragment.generateTag())
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun popFragments(depth: Int) {
        require(depth >= 1) { "Pop depth should be greater than 0" }

        val currentStack = fragmentStacks[selectedTabIndex]

        if (depth >= currentStack.size - 1) {
            clearStack()
            return
        }

        val transaction = fragmentManager.beginTransaction()

        for (i in 0 until depth) {
            fragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var fragment = reattachPreviousFragment(transaction)
        if (fragment == null) {
            fragment = currentStack.last()
            transaction.add(containerId, fragment, fragment.tag)
        }
        transaction.commit()

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun clearStack() {
        val currentStack = fragmentStacks[selectedTabIndex]

        if (currentStack.size <= 1) return

        val transaction = fragmentManager.beginTransaction()

        while (currentStack.size > 1) {
            fragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var fragment = reattachPreviousFragment(transaction)
        if (fragment == null) {
            fragment = currentStack.last()
            transaction.add(containerId, fragment, fragment.tag)
        }
        transaction.commit()

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun replace(fragment: Fragment) {
        if(getCurrentFragment() == null) return

        val transaction = fragmentManager.beginTransaction()

        val currentStack = fragmentStacks[selectedTabIndex]

        if (currentStack.isNotEmpty()) {
            currentStack.removeAt(currentStack.size - 1)
        }

        if ((fragment as? IMultiStackFragment)?.getIdentifierInStack() != null)
            currentStack.filter { (it as? IMultiStackFragment)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        fragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }

        transaction.replace(containerId, fragment, fragment.generateTag())
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun getCurrentFragment(): Fragment? {
        if (mCurrentFragment == null){
            mCurrentFragment = fragmentStacks.getOrNull(selectedTabIndex)?.lastOrNull()?.let { fragmentManager.findFragmentByTag(it.tag) }
        }
        return mCurrentFragment
    }

    fun isRootFragment() = fragmentStacks[selectedTabIndex].size == 1

    private fun getRootFragment(index: Int) =
        fragmentStacks.getOrNull(index)?.lastOrNull()?: throw IllegalStateException("No root fragment for index = $index")

    private fun reattachPreviousFragment(transaction: FragmentTransaction): Fragment? {
        val fragment = fragmentStacks.getOrNull(selectedTabIndex)?.lastOrNull()?.let { fragmentManager.findFragmentByTag(it.tag) }
        fragment?.let { transaction.attach(it) }
        return fragment
    }

    private fun executePendingTransactions() {
        if (!isTransactionExecuting) {
            isTransactionExecuting = true
            fragmentManager.executePendingTransactions()
            isTransactionExecuting = false
        }
    }

    private fun Fragment.generateTag()= this::class.java.name + (++tagCounter)

    fun saveInstanceState(outState: Bundle) {
        outState.putInt(Constants.Extras.TAG_COUNTER, tagCounter)
        outState.putString(Constants.Extras.CURRENT_FRAGMENT_TAG, mCurrentFragment?.tag)

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

    private fun restoreInstanceState(savedInstanceState: Bundle?, fragmentInitializers: List<() -> Fragment>): Boolean {
        if (savedInstanceState == null) return false

        tagCounter = savedInstanceState.getInt(Constants.Extras.TAG_COUNTER)
        mCurrentFragment = fragmentManager.findFragmentByTag(savedInstanceState.getString(Constants.Extras.CURRENT_FRAGMENT_TAG))

        try {
            val stackArrays = JSONArray(savedInstanceState.getString(Constants.Extras.FRAGMENT_STACKS))

            for (x in 0 until stackArrays.length()) {
                val stackArray = stackArrays.getJSONArray(x)
                val stack = mutableListOf<Fragment>()
                if (stackArray.length() == 1) {
                    val fragment = stackArray.getString(0)?.takeUnless { it.equals("null", true) }
                        ?.let { fragmentManager.findFragmentByTag(it) }?: fragmentInitializers.getOrNull(x)?.invoke()
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

        fun setState(state: Bundle?) = apply { savedInstanceState = state }

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionListener(listener: TransactionListener) = apply { transactionListener = listener }

        fun build() = MultiStacks(this)
    }
}
