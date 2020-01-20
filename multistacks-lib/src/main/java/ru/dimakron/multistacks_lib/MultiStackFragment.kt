package ru.dimakron.multistacks_lib

import android.content.Context
import androidx.fragment.app.Fragment

abstract class MultiStackFragment: Fragment(), IMultiStackFragment{

    protected var multiStackActivity: IMultiStackActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        multiStackActivity = context as? IMultiStackActivity
    }

}