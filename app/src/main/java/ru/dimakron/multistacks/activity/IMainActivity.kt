package ru.dimakron.multistacks.activity

import androidx.fragment.app.Fragment

interface IMainActivity {

    fun pushFragment(fragment: Fragment)

    fun switchToHome()

    fun replaceWithProfile()

    fun clearStack()

}