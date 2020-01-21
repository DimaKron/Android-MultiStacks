package ru.dimakron.multistacks.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_favourites.*
import ru.dimakron.multistacks.Constants
import ru.dimakron.multistacks.R
import ru.dimakron.multistacks.activity.IMainActivity
import ru.dimakron.multistacks_lib.IMultiStackFragment

class FavouritesFragment: Fragment(), IMultiStackFragment {

    companion object{
        fun newInstance(depth: Int = 0) = FavouritesFragment().apply {
            arguments = Bundle().apply {
                putInt(Constants.Extras.DEPTH, depth)
            }
        }
    }

    private var mainActivity: IMainActivity? = null

    private var depth: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as? IMainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depth = arguments?.getInt(Constants.Extras.DEPTH)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_favourites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.setTitle(R.string.main_item_favourites)

        depthTextView.text = getString(R.string.simple_text_depth, depth)

        addButton.setOnClickListener { mainActivity?.pushFragment(newInstance((depth ?: 0) + 1)) }
        clearButton.setOnClickListener { mainActivity?.clearStack() }
    }

}