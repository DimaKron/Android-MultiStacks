package ru.dimakron.multistacks

import androidx.fragment.app.Fragment

interface IMainActivity {

    fun pushFragment(fragment: Fragment)

    fun switchToHome()

    fun switchToNewHome()

}