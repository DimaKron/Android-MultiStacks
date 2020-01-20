package ru.dimakron.multistacks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_simple.*

class SimpleFragment: Fragment() {

    companion object{
        fun newInstance(tabName: String, depth: Int = 0) = SimpleFragment().apply {
            arguments = Bundle().apply {
                putString(Constants.Extras.TAB_NAME, tabName)
                putInt(Constants.Extras.DEPTH, depth)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_simple, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabName = arguments?.getString(Constants.Extras.TAB_NAME)

        activity?.title = tabName

        tabNameTextView.text = getString(R.string.simple_text_tab_name, tabName)
        depthTextView.text = getString(R.string.simple_text_depth, arguments?.getInt(Constants.Extras.DEPTH))

        addFragmentButton.setOnClickListener { /* TODO */ }
    }

}