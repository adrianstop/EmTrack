package com.example.emtrack.ui.live

import android.app.Activity
import android.content.Context
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
    private lateinit var activity: Activity
    private lateinit var textTMState: TextView
    private lateinit var textTMAcc: TextView
    private lateinit var textLiveStatus: TextView

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

        val textView: TextView = binding.textLiveExp
        liveViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }



        return root
    }

    override fun onResume() {
        super.onResume()
        textTMState = requireView().findViewById<TextView>(R.id.text_live_state)
        textTMAcc = requireView().findViewById<TextView>(R.id.text_live_accuracy)
        textLiveStatus = requireView().findViewById<TextView>(R.id.text_live_status)
    }
    fun updateUIElements(state: String, accuracy: Float, liveStatus: String) {
        textTMState.text = resources.getString(R.string.live_state, state)
        textTMAcc.text = resources.getString(R.string.live_state_accuracy, accuracy * 100)
        textLiveStatus.text = resources.getString(R.string.live_status, liveStatus)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}