package com.example.klasifikasijalan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.klasifikasijalan.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UiAnimations.staggerFadeUp(binding.contentContainer)

        binding.btnStartDetection.setOnClickListener {
            UiAnimations.pulse(it)
            it.postDelayed({
                (activity as? MainActivity)?.replaceFragment(DetectionFragment())
            }, 150)
        }

        binding.btnAbout.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(AboutFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
