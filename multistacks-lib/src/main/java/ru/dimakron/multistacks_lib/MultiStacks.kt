package ru.dimakron.multistacks_lib

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class MultiStacks private constructor(builder: Builder) {

    private val containerId = builder.containerId
    private val fragmentManager = builder.fragmentManager
    private val transactionListener = builder.transactionListener
    private val transactionOptions = builder.transactionOptions

    private val fragmentStacks = mutableListOf<MutableList<Fragment>>()
    private var selectedTabIndex = builder.selectedTabIndex
    private var isTransactionExecuting = false
    private var mCurrentFragment: Fragment? = null // TODO Rename?
    private var tagCounter = 0

    init {
        //if (!restoreFromBundle(builder.savedInstanceState, builder.mRootFragments)) { TODO Временно закомментированно
        builder.rootFragmentInitializers.forEach { fragmentStacks.add(mutableListOf(it())) }

        val transaction = createTransaction()

        fragmentManager.fragments.forEach { transaction.remove(it) }

        val fragment = getRootFragment(selectedTabIndex)
        transaction.add(containerId, fragment, fragment.generateTag())
        transaction.commit()

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onTabTransaction(mCurrentFragment, selectedTabIndex)
        //}
    }

    fun setSelectedTabIndex(index: Int){
        require(index >= 0 && index < fragmentStacks.size) { "Tab index should be in range [0, ${fragmentStacks.size - 1} but is $index]" }

        if (selectedTabIndex == index) return

        selectedTabIndex = index

        val transaction = createTransaction()

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

        val transaction = createTransaction()

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

        val transaction = createTransaction()

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

    // Current root fragment replaces with rootFragment if rootFragment != null
    fun clearStack(rootFragment: Fragment? = null) {
        val currentStack = fragmentStacks[selectedTabIndex]

        if (currentStack.size <= 1 && rootFragment == null || currentStack.size <= 0 && rootFragment != null) return

        val transaction = createTransaction()

        while (currentStack.size > 1 && rootFragment == null || currentStack.size > 0 && rootFragment != null) {
            fragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var pushToStack = false
        var fragment = reattachPreviousFragment(transaction)
        if (fragment == null) {
            if (currentStack.isNotEmpty()) {
                fragment = currentStack[currentStack.size - 1]
                transaction.add(containerId, fragment, fragment.tag)
            } else {
                fragment = rootFragment?: getRootFragment(selectedTabIndex)
                transaction.add(containerId, fragment, fragment.generateTag())
                pushToStack = true
            }
        }
        transaction.commit()

        executePendingTransactions()

        if (pushToStack && rootFragment != null) currentStack.add(fragment)

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun replace(fragment: Fragment) {
        if(getCurrentFragment() == null) return

        val transaction = createTransaction()

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

    private fun createTransaction(): FragmentTransaction {
        val transaction = fragmentManager.beginTransaction()

        if (transactionOptions != null) {
            transaction.setCustomAnimations(transactionOptions.enterAnimation, transactionOptions.exitAnimation, transactionOptions.popEnterAnimation, transactionOptions.popExitAnimation)
            transaction.setTransitionStyle(transactionOptions.transitionStyle)

            transaction.setTransition(transactionOptions.transition)

            transactionOptions.sharedElements?.filter { it.first != null && it.second != null }?.forEach { transaction.addSharedElement(it.first!!, it.second!!) }
            transactionOptions.breadCrumbTitle?.let { transaction.setBreadCrumbTitle(it) }
            transactionOptions.breadCrumbShortTitle?.let { transaction.setBreadCrumbShortTitle(it) }
        }

        return transaction
    }

    private fun executePendingTransactions() {
        if (!isTransactionExecuting) {
            isTransactionExecuting = true
            fragmentManager.executePendingTransactions()
            isTransactionExecuting = false
        }
    }

    private fun Fragment.generateTag()= this::class.java.name + (++tagCounter)

    /*fun onSaveInstanceState(outState: Bundle) { TODO Временно закомментированно
        outState.putInt(Constants.Extras.FragNavController.TAG_COUNT, mTagCount)
        outState.putInt(Constants.Extras.FragNavController.SELECTED_TAB_INDEX, mSelectedTabIndex)
        mCurrentFrag?.let { outState.putString(Constants.Extras.FragNavController.CURRENT_FRAGMENT, it.tag)  }

        try {
            val stackArrays = JSONArray()
            mFragmentStacks.forEach { stack ->
                val stackArray = JSONArray()
                stack.forEach { stackArray.put(it.tag) }
                stackArrays.put(stackArray)
            }
            outState.putString(Constants.Extras.FragNavController.FRAGMENT_STACK, stackArrays.toString())
        } catch (t: Throwable) {
            // Nothing we can do
        }
    }

    private fun restoreFromBundle(savedInstanceState: Bundle?, rootFragments: List<Fragment>?): Boolean {
        if (savedInstanceState == null) return false

        mTagCount = savedInstanceState.getInt(Constants.Extras.FragNavController.TAG_COUNT, 0)
        mCurrentFrag = mFragmentManager.findFragmentByTag(savedInstanceState.getString(Constants.Extras.FragNavController.CURRENT_FRAGMENT))

        try {
            val stackArrays = JSONArray(savedInstanceState.getString(Constants.Extras.FragNavController.FRAGMENT_STACK))

            for (x in 0 until stackArrays.length()) {
                val stackArray = stackArrays.getJSONArray(x)
                val stack = mutableListOf<Fragment>()
                if (stackArray.length() == 1) {
                    val tag = stackArray.getString(0)
                    val fragment = if (tag == null || "null".equals(tag, true)) rootFragments?.getOrNull(x)?: getRootFragment(x) else mFragmentManager.findFragmentByTag(tag)
                    fragment?.let { stack.add(it) } // Скорее всего вызывает баг. Удалить по возможности
                } else {
                    for (y in 0 until stackArray.length()) {
                        val tag = stackArray.getString(y)
                        if (tag != null && !"null".equals(tag, true)) {
                            mFragmentManager.findFragmentByTag(tag)?.let { stack.add(it) }
                        }
                    }
                }
                mFragmentStacks.add(stack)
            }

            val tabIndex = savedInstanceState.getInt(Constants.Extras.FragNavController.SELECTED_TAB_INDEX)
            if (tabIndex in 0 until MAX_TABS){
                switchTab(tabIndex)
            }

            return true
        } catch (t: Throwable) {
            return false
        }
    } */

    interface TransactionListener {
        fun onTabTransaction(fragment: Fragment?, index: Int)
        fun onFragmentTransaction(fragment: Fragment?)
    }

    class Builder(val fragmentManager: FragmentManager,
                  @IdRes val containerId: Int) {

        var savedInstanceState: Bundle? = null
        val rootFragmentInitializers = mutableListOf<() -> Fragment>()
        var selectedTabIndex = 0
        var transactionOptions: TransactionOptions? = null
        var transactionListener: TransactionListener? = null

        fun setState(state: Bundle?) = apply { savedInstanceState = state }

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionOptions(options: TransactionOptions?) = apply { transactionOptions = options }

        fun setTransactionListener(listener: TransactionListener) = apply { transactionListener = listener }

        fun build() = MultiStacks(this)
    }
}
