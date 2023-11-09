package com.alexxx.coolnavigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alexxx.coolnavigation.R
import com.alexxx.coolnavigation.extensions.navigate
import kotlinx.android.synthetic.main.fragment.*

class DashboardFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        requireActivity().title = getString(R.string.title_dashboard)
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val count = HomeFragmentArgs.fromBundle(requireArguments()).count
        textView.text = IntRange(0, count).joinToString(separator = "->")

        openNextButton.setOnClickListener {
            navigate(DashboardFragmentDirections.actionDashboardFragmentSelf(count + 1))
        }

    }
}