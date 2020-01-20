package ru.dimakron.multistacks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_simple.*
import ru.dimakron.multistacks_lib.IMultiStackFragment

class SimpleFragment: Fragment(), IMultiStackFragment {

    companion object{
        fun newInstance(tabName: String, depth: Int = 0) = SimpleFragment().apply {
            arguments = Bundle().apply {
                putString(Constants.Extras.TAB_NAME, tabName)
                putInt(Constants.Extras.DEPTH, depth)
            }
        }
    }

    private var mainActivity: IMainActivity? = null

    private var tabName: String? = null
    private var depth: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as? IMainActivity
    }

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

        addFragmentButton.setOnClickListener { mainActivity?.pushFragment(newInstance(tabName.toString(), (depth?: 0) + 1)) }
        switchToHomeButton.setOnClickListener { mainActivity?.switchToHome() }
        switchToNewHomeButton.setOnClickListener { mainActivity?.switchToNewHome() }
    }

}