package com.example.vplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vplayer.MainActivity
import com.example.vplayer.R
import com.example.vplayer.adapter.VideoAdapter
import com.example.vplayer.databinding.FragmentVideoBinding

class VideoFragment : Fragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_video,container,false)

        val binding = FragmentVideoBinding.bind(view)

        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(10)
        binding.videoRV.layoutManager = LinearLayoutManager(requireContext())
        binding.videoRV.adapter = VideoAdapter(requireContext(),MainActivity.videoList)
        binding.totalVds.text = "Total Folders: ${MainActivity.videoList.size}"
        return view
    }


}