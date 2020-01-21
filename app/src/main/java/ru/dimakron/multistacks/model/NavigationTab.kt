package ru.dimakron.multistacks.model

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment

class NavigationTab(@IdRes val tabId: Int,
                    val fragmentInitializer: () -> Fragment)