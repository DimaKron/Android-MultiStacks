package ru.dimakron.multistacks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_simple.*
import ru.dimakron.multistacks_lib.MultiStackFragment

class SimpleFragment: MultiStackFragment() {

    companion object{
        fun newInstance(tabName: String, depth: Int = 0) = SimpleFragment().apply {
            arguments = Bundle().apply {
                putString(Constants.Extras.TAB_NAME, tabName)
                putInt(Constants.Extras.DEPTH, depth)
            }
        }
    }

    private var tabName: String? = null
    private var depth: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabName = arguments?.getString(Constants.Extras.TAB_NAME)
        depth = arguments?.getInt(Constants.Extras.DEPTH)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_simple, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = tabName

        tabNameTextView.text = getString(R.string.simple_text_tab_name, tabName)
        depthTextView.text = getString(R.string.simple_text_depth, depth)

        addFragmentButton.setOnClickListener { multiStackActivity?.pushFragment(newInstance(tabName.toString(), (depth?: 0) + 1)) }
    }

}