package com.example.vplayer.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vplayer.MainActivity
import com.example.vplayer.R
import com.example.vplayer.adapter.FolderAdapter
import com.example.vplayer.databinding.FragmentFolderBinding


class FolderFragment : Fragment() {


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_folder,container,false)


        val binding = FragmentFolderBinding.bind(view)

        binding.folderRV.setHasFixedSize(true)
        binding.folderRV.setItemViewCacheSize(10)
        binding.folderRV.layoutManager = LinearLayoutManager(requireContext())
        binding.folderRV.adapter = FolderAdapter(requireContext(), MainActivity.folderList)
        binding.totalFolders.text = "Total Folders: ${MainActivity.folderList.size}"

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val layoutManager = binding.folderRV.layoutManager as LinearLayoutManager
                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    requireActivity().finish()
                } else {
                    binding.folderRV.scrollToPosition(0)
                }
            }
        })
        return view
    }

}