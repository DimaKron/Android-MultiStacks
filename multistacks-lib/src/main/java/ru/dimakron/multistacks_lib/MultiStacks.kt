package ru.dimakron.multistacks_lib

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MultiStacks private constructor(builder: Builder) {

    /*companion object{
        const val MAX_TABS = 5

        fun newBuilder(savedInstanceState: Bundle?, fragmentManager: FragmentManager, containerId: Int) = Builder(savedInstanceState, fragmentManager, containerId)
    }

    private val mContainerId = builder.mContainerId
    private val mFragmentManager = builder.mFragmentManager
    private val mFragmentStacks = mutableListOf<MutableList<Fragment>>()
    private val mRootFragmentListener = builder.mRootFragmentListener
    private val mTransactionListener = builder.mTransactionListener
    private val mDefaultTransactionOptions = builder.mDefaultTransactionOptions
    private var mSelectedTabIndex = builder.mSelectedTabIndex

    private var mCurrentFrag: Fragment? = null
    private var mCurrentDialogFrag: DialogFragment? = null
    private var mTagCount = 0
    private var mExecutingTransaction = false

    init {
        if (!restoreFromBundle(savedInstanceState, builder.mRootFragments)) {
            for (i in 0 until builder.mNumberOfTabs) {
                val stack = mutableListOf<Fragment>()
                builder.mRootFragments?.getOrNull(i)?.let { stack.add(it) }
                mFragmentStacks.add(stack)
            }
            initialize(builder.mSelectedTabIndex)
        }
    }

    fun switchTab(index: Int, transactionOptions: FragNavTransactionOptions? = null){
        require(index < mFragmentStacks.size) { "Can't switch to a tab that hasn't been initialized, Index: $index, current stack size: ${mFragmentStacks.size}. " +
                "Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener." }

        if (mSelectedTabIndex == index) return

        mSelectedTabIndex = index

        val transaction = createTransactionWithOptions(transactionOptions)

        getCurrentFrag()?.let { transaction.detach(it) }

        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            fragment = getRootFragment(mSelectedTabIndex)
            transaction.add(mContainerId, fragment, generateTag(fragment))
            transaction.commit()
        }

        executePendingTransactions()

        mCurrentFrag = fragment
        mTransactionListener?.onTabTransaction(mCurrentFrag, mSelectedTabIndex)
    }

    fun pushFragment(fragment: Fragment?, transactionOptions: FragNavTransactionOptions? = null) {
        if (fragment == null) return

        val currentStack = mFragmentStacks[mSelectedTabIndex]

        val transaction = createTransactionWithOptions(transactionOptions)

        getCurrentFrag()?.let { transaction.detach(it) }

        if ((fragment as? BaseFragment<*>)?.getIdentifierInStack() != null){
            currentStack.filter { (it as? BaseFragment<*>)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        mFragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }
        }

        transaction.add(mContainerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)

        mCurrentFrag = fragment
        mTransactionListener?.onFragmentTransaction(mCurrentFrag, TransactionType.PUSH)
    }

    fun popFragments(popDepth: Int, transactionOptions: FragNavTransactionOptions? = null) {
        check(!isRootFragment()) { "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)" }
        require(popDepth >= 1) { "popFragments parameter needs to be greater than 0" }

        val currentStack = mFragmentStacks[mSelectedTabIndex]

        if (popDepth >= currentStack.size - 1) {
            clearStack(transactionOptions)
            return
        }

        val transaction = createTransactionWithOptions(transactionOptions)

        for (i in 0 until popDepth) {
            mFragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var bShouldPush = false
        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            if (currentStack.isNotEmpty()) {
                fragment = currentStack[currentStack.size - 1]
                transaction.add(mContainerId, fragment, fragment.tag)
                transaction.commit()
            } else {
                fragment = getRootFragment(mSelectedTabIndex)
                transaction.add(mContainerId, fragment, generateTag(fragment))
                transaction.commit()
                bShouldPush = true
            }
        }

        executePendingTransactions()

        // Скорее всего вызывает баг. Удалить по возможности
        if (bShouldPush) {
            currentStack.add(fragment)
        }

        mCurrentFrag = fragment
        mTransactionListener?.onFragmentTransaction(mCurrentFrag, TransactionType.POP)
    }

    /*
    * Если rootFragment != null, то стек очищается вместе с корневым фрагментом и на его место помещается rootFragment
    * */
    fun clearStack(transactionOptions: FragNavTransactionOptions? = null, rootFragment: Fragment? = null) {
        val currentStack = mFragmentStacks[mSelectedTabIndex]

        if (currentStack.size <= 1 && rootFragment == null || currentStack.size <= 0 && rootFragment != null) return

        val transaction = createTransactionWithOptions(transactionOptions)

        while (currentStack.size > 1 && rootFragment == null || currentStack.size > 0 && rootFragment != null) {
            mFragmentManager.findFragmentByTag(currentStack.removeAt(currentStack.size - 1).tag)?.let { transaction.remove(it) }
        }

        var bShouldPush = false
        var fragment = reattachPreviousFragment(transaction)
        if (fragment != null) {
            transaction.commit()
        } else {
            if (currentStack.isNotEmpty()) {
                fragment = currentStack[currentStack.size - 1]
                transaction.add(mContainerId, fragment, fragment.tag)
                transaction.commit()
            } else {
                fragment = rootFragment?: getRootFragment(mSelectedTabIndex)
                transaction.add(mContainerId, fragment, generateTag(fragment))
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

        mCurrentFrag = fragment
        mTransactionListener?.onFragmentTransaction(mCurrentFrag, TransactionType.POP)
    }

    fun replaceFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = null) {
        if(getCurrentFrag() == null) return

        val transaction = createTransactionWithOptions(transactionOptions)

        val currentStack = mFragmentStacks[mSelectedTabIndex]

        if (currentStack.isNotEmpty()) {
            currentStack.removeAt(currentStack.size - 1)
        }

        // Работа этой конструкции внутри этого метода не проверялась
        if ((fragment as? BaseFragment<*>)?.getIdentifierInStack() != null){
            currentStack.filter { (it as? BaseFragment<*>)?.getIdentifierInStack() == fragment.getIdentifierInStack() }
                    .forEach { f ->
                        currentStack.remove(f)
                        mFragmentManager.findFragmentByTag(f.tag)?.let { transaction.remove(it) }
                    }
        }

        transaction.replace(mContainerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        currentStack.add(fragment)
        mCurrentFrag = fragment
        mTransactionListener?.onFragmentTransaction(mCurrentFrag, TransactionType.REPLACE)
    }

    fun getCurrentDialogFrag(): DialogFragment? {
        if (mCurrentDialogFrag != null) {
            return mCurrentDialogFrag
        } else {
            val fragmentManager = mCurrentFrag?.childFragmentManager ?: mFragmentManager
            fragmentManager.fragments.find { it is DialogFragment }?.let { mCurrentDialogFrag = it as DialogFragment }
        }
        return mCurrentDialogFrag
    }

    fun clearDialogFragment() {
        if (mCurrentDialogFrag != null) {
            mCurrentDialogFrag?.dismiss()
            mCurrentDialogFrag = null
        } else {
            val fragmentManager = mCurrentFrag?.childFragmentManager ?: mFragmentManager
            fragmentManager.fragments.forEach{ (it as? DialogFragment)?.dismiss() }
        }
    }

    fun showDialogFragment(dialogFragment: DialogFragment?) {
        if (dialogFragment == null) return

        val fragmentManager = mCurrentFrag?.childFragmentManager?: mFragmentManager
        fragmentManager.fragments.forEach { (it as? DialogFragment)?.dismiss() }

        mCurrentDialogFrag = dialogFragment
        try {
            dialogFragment.show(fragmentManager, dialogFragment::class.java.simpleName)
        } catch (ex: IllegalStateException) {
            // Activity was likely destroyed before we had a chance to show, nothing can be done here.
        }
    }

    private fun initialize(index: Int) {
        mSelectedTabIndex = index

        require(mSelectedTabIndex <= mFragmentStacks.size) { "Starting index cannot be larger than the number of stacks" }

        clearFragmentManager()
        clearDialogFragment()

        val transaction = createTransactionWithOptions()

        val fragment = getRootFragment(index)
        transaction.add(mContainerId, fragment, generateTag(fragment))
        transaction.commit()

        executePendingTransactions()

        mCurrentFrag = fragment
        mTransactionListener?.onTabTransaction(mCurrentFrag, mSelectedTabIndex)
    }

    private fun getRootFragment(index: Int): Fragment {
        var fragment: Fragment? = null

        val stack = mFragmentStacks[index]
        if (stack.isNotEmpty()) {
            fragment = stack[stack.size - 1]
        } else if (mRootFragmentListener != null) {
            fragment = mRootFragmentListener.getRootFragment(index)
            stack.add(fragment)
        }

        checkNotNull(fragment) { "Either you haven't past in a fragment at this index in your constructor, or you haven't " +
                "provided a way to create it while via your RootFragmentListener.getRootFragment(index)" }

        return fragment
    }

    private fun reattachPreviousFragment(ft: FragmentTransaction): Fragment? {
        var fragment: Fragment? = null

        val currentStack = mFragmentStacks[mSelectedTabIndex]
        if (currentStack.isNotEmpty()) {
            fragment = mFragmentManager.findFragmentByTag(currentStack[currentStack.size - 1].tag)
            fragment?.let { ft.attach(it) }
        }

        return fragment
    }

    fun getCurrentFrag(): Fragment? {
        if (mCurrentFrag != null) {
            return mCurrentFrag
        } else {
            val currentStack = mFragmentStacks[mSelectedTabIndex]
            if (currentStack.isNotEmpty()) {
                mCurrentFrag = mFragmentManager.findFragmentByTag(currentStack[currentStack.size - 1].tag)
            }
        }
        return mCurrentFrag
    }

    private fun generateTag(fragment: Fragment)= fragment::class.java.name + (++mTagCount)

    private fun executePendingTransactions() {
        if (!mExecutingTransaction) {
            mExecutingTransaction = true
            mFragmentManager.executePendingTransactions()
            mExecutingTransaction = false
        }
    }

    private fun clearFragmentManager() {
        val transaction = createTransactionWithOptions()

        mFragmentManager.fragments.forEach { transaction.remove(it) }

        transaction.commit()
        executePendingTransactions()
    }

    private fun createTransactionWithOptions(options: FragNavTransactionOptions? = null): FragmentTransaction {
        val transaction = mFragmentManager.beginTransaction()

        val transactionOptions = options?: mDefaultTransactionOptions

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

    fun getCurrentStackIndex() = mSelectedTabIndex

    fun isRootFragment() = mFragmentStacks[mSelectedTabIndex].size == 1

    fun onSaveInstanceState(outState: Bundle) {
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
    }

    interface RootFragmentListener {
        fun getRootFragment(index: Int): Fragment
    }

    interface TransactionListener {
        fun onTabTransaction(fragment: Fragment?, index: Int)
        fun onFragmentTransaction(fragment: Fragment?, transactionType: TransactionType)
    } */

    class Builder(val fragmentManager: FragmentManager,
                  @IdRes val containerId: Int) {

        var savedInstanceState: Bundle? = null
        val rootFragmentInitializers = mutableListOf<() -> Fragment>()
        var selectedTabIndex = 0
        var transactionOptions: FragNavTransactionOptions? = null
        //var mTransactionListener: TransactionListener? = null TODO

        fun setState(state: Bundle?) = apply { savedInstanceState = state }

        fun setRootFragmentInitializers(initializers: List<() -> Fragment>) = apply { rootFragmentInitializers.replaceWith(initializers) }

        fun setSelectedTabIndex(index: Int) = apply { selectedTabIndex = index }

        fun setTransactionOptions(options: FragNavTransactionOptions?) = apply { transactionOptions = options }

        //fun transactionListener(listener: TransactionListener) = apply { mTransactionListener = listener } TODO

        fun build() = MultiStacks(this)
    }
}
