package com.example.emtrack.ui.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.emtrack.R
import com.example.emtrack.databinding.FragmentLiveBinding

class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val liveViewModel =
            ViewModelProvider(this).get(LiveViewModel::class.java)

        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textLiveStatus
        liveViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}