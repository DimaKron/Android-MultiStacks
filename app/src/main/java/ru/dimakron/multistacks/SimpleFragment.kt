package ru.dimakron.multistacks

import android.os.Bundle
import androidx.fragment.app.Fragment

class SimpleFragment: Fragment() {

    companion object{
        fun newInstance() = SimpleFragment().apply { arguments = Bundle() }
    }

}