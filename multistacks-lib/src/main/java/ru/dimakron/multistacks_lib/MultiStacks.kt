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
    private val defaultTransactionOptions = builder.transactionOptions

    private val fragmentStacks = mutableListOf<MutableList<Fragment>>()
    private var selectedTabIndex = builder.selectedTabIndex
    private var isTransactionExecuting = false
    private var mCurrentFragment: Fragment? = null // TODO Rename? Needed?
    private var tagCounter = 0

    init {
        //if (!restoreFromBundle(builder.savedInstanceState, builder.mRootFragments)) { TODO Временно закомментированно
            for (initializer in builder.rootFragmentInitializers) {
                val stack = mutableListOf<Fragment>()
                stack.add(initializer())
                fragmentStacks.add(stack)
            }
            initialize(builder.selectedTabIndex)
        //}
    }

    fun switchTab(index: Int, transactionOptions: FragNavTransactionOptions? = null){
        require(index < fragmentStacks.size) { "Can't switch to a tab that hasn't been initialized, Index: $index, current stack size: ${fragmentStacks.size}. " +
                "Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener." }

        if (selectedTabIndex == index) return

        selectedTabIndex = index

        val transaction = createTransactionWithOptions(transactionOptions)

        getCurrentFrag()?.let { transaction.detach(it) }

        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            fragment = getRootFragment(selectedTabIndex)
            transaction.add(containerId, fragment, generateTag(fragment))
            transaction.commit()
        }

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onTabTransaction(mCurrentFragment, selectedTabIndex)
    }

    fun pushFragment(fragment: Fragment?, transactionOptions: FragNavTransactionOptions? = null) {
        if (fragment == null) return

        val currentStack = fragmentStacks[selectedTabIndex]

        val transaction = createTransactionWithOptions(transactionOptions)

        getCurrentFrag()?.let { transaction.detach(it) }

        /*if ((fragment as? BaseFragment<*>)?.getIdentifierInStack() != null){ TODO Временно закомментированно
            currentStack.filter { (it as? BaseFragment<*>)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        mFragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }
        }*/

        transaction.add(containerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun popFragments(popDepth: Int, transactionOptions: FragNavTransactionOptions? = null) {
        check(!isRootFragment()) { "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)" }
        require(popDepth >= 1) { "popFragments parameter needs to be greater than 0" }

        val currentStack = fragmentStacks[selectedTabIndex]

        if (popDepth >= currentStack.size - 1) {
            clearStack(transactionOptions)
            return
        }

        val transaction = createTransactionWithOptions(transactionOptions)

        for (i in 0 until popDepth) {
            fragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var bShouldPush = false
        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            if (currentStack.isNotEmpty()) {
                fragment = currentStack[currentStack.size - 1]
                transaction.add(containerId, fragment, fragment.tag)
                transaction.commit()
            } else {
                fragment = getRootFragment(selectedTabIndex)
                transaction.add(containerId, fragment, generateTag(fragment))
                transaction.commit()
                bShouldPush = true
            }
        }

        executePendingTransactions()

        // Скорее всего вызывает баг. Удалить по возможности
        if (bShouldPush) {
            currentStack.add(fragment)
        }

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    /*
    * Если rootFragment != null, то стек очищается вместе с корневым фрагментом и на его место помещается rootFragment
    * */
    fun clearStack(transactionOptions: FragNavTransactionOptions? = null, rootFragment: Fragment? = null) {
        val currentStack = fragmentStacks[selectedTabIndex]

        if (currentStack.size <= 1 && rootFragment == null || currentStack.size <= 0 && rootFragment != null) return

        val transaction = createTransactionWithOptions(transactionOptions)

        while (currentStack.size > 1 && rootFragment == null || currentStack.size > 0 && rootFragment != null) {
            fragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var bShouldPush = false
        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            if (currentStack.isNotEmpty()) {
                fragment = currentStack[currentStack.size - 1]
                transaction.add(containerId, fragment, fragment.tag)
                transaction.commit()
            } else {
                fragment = rootFragment?: getRootFragment(selectedTabIndex)
                transaction.add(containerId, fragment, generateTag(fragment))
                transaction.commit()
                bShouldPush = true
            }
        }

        executePendingTransactions()

        if (bShouldPush && rootFragment != null) currentStack.add(fragment)

        /* Удалено, из-за того, что вызывало баг. Метод getRootFragment сам по себе добавляет фрагмент в стек
        if (bShouldPush) {
            currentStack.add(fragment)
        }*/

        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    fun replaceFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = null) {
        if(getCurrentFrag() == null) return

        val transaction = createTransactionWithOptions(transactionOptions)

        val currentStack = fragmentStacks[selectedTabIndex]

        if (currentStack.isNotEmpty()) {
            currentStack.removeAt(currentStack.size - 1)
        }

        // Работа этой конструкции внутри этого метода не проверялась
        /*if ((fragment as? BaseFragment<*>)?.getIdentifierInStack() != null){ TODO Временно закомментированно
            currentStack.filter { (it as? BaseFragment<*>)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        mFragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }
        }*/

        transaction.replace(containerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)
        mCurrentFragment = fragment
        transactionListener?.onFragmentTransaction(mCurrentFragment)
    }

    private fun initialize(index: Int) {
        selectedTabIndex = index

        require(selectedTabIndex <= fragmentStacks.size) { "Starting index cannot be larger than the number of stacks" }

        clearFragmentManager()

        val transaction = createTransactionWithOptions()

        val fragment = getRootFragment(index)
        transaction.add(containerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        mCurrentFragment = fragment
        transactionListener?.onTabTransaction(mCurrentFragment, selectedTabIndex)
    }

    private fun getRootFragment(index: Int): Fragment {
        var fragment: Fragment? = null

        val stack = fragmentStacks[index]
        if (stack.isNotEmpty()) {
            fragment = stack[stack.size - 1]
        }

        checkNotNull(fragment) { "Either you haven't past in a fragment at this index in your constructor, or you haven't " +
                "provided a way to create it while via your RootFragmentListener.getRootFragment(index)" }

        return fragment
    }

    private fun reattachPreviousFragment(ft: FragmentTransaction): Fragment? {
        var fragment: Fragment? = null

        val currentStack = fragmentStacks[selectedTabIndex]
        if (currentStack.isNotEmpty()) {
            fragment = fragmentManager.findFragmentByTag(currentStack[currentStack.size - 1].tag)
            fragment?.let { ft.attach(it) }
        }

        return fragment
    }

    fun getCurrentFrag(): Fragment? {
        if (mCurrentFragment != null) {
            return mCurrentFragment
        } else {
            val currentStack = fragmentStacks[selectedTabIndex]
            if (currentStack.isNotEmpty()) {
                mCurrentFragment = fragmentManager.findFragmentByTag(currentStack[currentStack.size - 1].tag)
            }
        }
        return mCurrentFragment
    }

    private fun generateTag(fragment: Fragment)= fragment::class.java.name + (++tagCounter)

    private fun executePendingTransactions() {
        if (!isTransactionExecuting) {
            isTransactionExecuting = true
            fragmentManager.executePendingTransactions()
            isTransactionExecuting = false
        }
    }

    private fun clearFragmentManager() {
        val transaction = createTransactionWithOptions()

        fragmentManager.fragments.forEach { transaction.remove(it) }

        transaction.commit()
        executePendingTransactions()
    }

    private fun createTransactionWithOptions(options: FragNavTransactionOptions? = null): FragmentTransaction {
        val transaction = fragmentManager.beginTransaction()

        val transactionOptions = options?: defaultTransactionOptions

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

    fun getCurrentStackIndex() = selectedTabIndex

    fun isRootFragment() = fragmentStacks[selectedTabIndex].size == 1

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
        var transactionOptions: FragNavTransactionOptions? = null
        var transactionListener: TransactionListener? = null

        fun setState(state: Bundle?) = apply { savedInstanceState = state }

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionOptions(options: FragNavTransactionOptions?) = apply { transactionOptions = options }

        fun setTransactionListener(listener: TransactionListener) = apply { transactionListener = listener }

        fun build() = MultiStacks(this)
    }
}
