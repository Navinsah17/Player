package com.example.vplayer.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vplayer.MainActivity
import com.example.vplayer.PlayerActivity
import com.example.vplayer.R
import com.example.vplayer.adapter.VideoAdapter
import com.example.vplayer.databinding.FragmentVideoBinding
import com.google.android.material.search.SearchView

class VideoFragment : Fragment() {

    lateinit var adapter: VideoAdapter
    lateinit var binding: FragmentVideoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_video,container,false)

        binding = FragmentVideoBinding.bind(view)

        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(10)
        binding.videoRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = VideoAdapter(requireContext(),MainActivity.videoList)
        binding.videoRV.adapter = adapter
        binding.totalVds.text = "Total Videos: ${MainActivity.videoList.size}"
        binding.nowplayingBtn.setOnClickListener {

            val intent =  Intent(requireContext(),PlayerActivity::class.java)
            intent.putExtra("class","NowPlaying")
            startActivity(intent)
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view, menu)
        val searchView = menu.findItem(R.id.searchView)?.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    MainActivity.searchList = ArrayList()
                    for(video in MainActivity.videoList){
                        if(video.title.lowercase().contains(newText.lowercase()))
                            MainActivity.searchList.add(video)
                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)
                }
                return true
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(PlayerActivity.position != -1) binding.nowplayingBtn.visibility = View.VISIBLE
        if(MainActivity.dataChanged) adapter.notifyDataSetChanged()
        MainActivity.dataChanged = false
//
    }


}