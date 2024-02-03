package com.example.vplayer.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vplayer.MainActivity
import com.example.vplayer.MainActivity.Companion.sortValue
import com.example.vplayer.PlayerActivity
import com.example.vplayer.R
import com.example.vplayer.adapter.VideoAdapter
import com.example.vplayer.databinding.FragmentVideoBinding
import com.example.vplayer.dataclass.Video
import com.example.vplayer.dataclass.getAllVideo
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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


        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val layoutManager = binding.videoRV.layoutManager as LinearLayoutManager
                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                    requireActivity().finish()
                } else {
                    binding.videoRV.scrollToPosition(0)
                }
            }
        })

        //refresher

        binding.root.setOnRefreshListener {
            MainActivity.videoList = getAllVideo(requireContext())
            adapter.updateList(MainActivity.videoList)
            binding.totalVds.text = "Total Videos: ${MainActivity.videoList.size}"

            binding.root.isRefreshing = false
        }
        binding.nowplayingBtn.setOnClickListener {

            val intent =  Intent(requireContext(),PlayerActivity::class.java)
            intent.putExtra("class","NowPlaying")
            startActivity(intent)
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view, menu)
        val searchItem = menu.findItem(R.id.searchView)
        val searchView = searchItem?.actionView as androidx.appcompat.widget.SearchView
        val searchEditTextId = androidx.appcompat.R.id.search_src_text
        val searchEditText = searchView.findViewById<EditText>(searchEditTextId)
        searchEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        //val searchView = menu.findItem(R.id.searchView)?.actionView as androidx.appcompat.widget.SearchView
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
        val sortOrderMenuItem = menu.findItem(R.id.sortOrderNav)
        sortOrderMenuItem?.setOnMenuItemClickListener {
            onOptionsItemSelected(sortOrderMenuItem)
            true
        }
/*//        inflater.inflate(R.menu.search_view, menu)
//        super.onCreateOptionsMenu(menu, inflater)*/

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sortOrderNav -> {
                showSortDialog()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showSortDialog() {
        val menuItems = arrayOf(
            "Latest",
            "Oldest",
            "Name(A to Z)",
            "Name(Z to A)",
            "File Size(Smallest)",
            "File Size(Largest)"
        )

        var value = sortValue // Initialize with the current sortValue
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort By")
            .setPositiveButton("OK") { _, _ ->
                // Save the selected sort value to SharedPreferences
                val sortEditor =
                    requireContext().getSharedPreferences("Sorting", AppCompatActivity.MODE_PRIVATE)
                        .edit()
                sortEditor.putInt("sortValue", value)
                sortEditor.apply()

                // Apply sorting based on the selected value
                applySorting(value)
            }
            .setSingleChoiceItems(menuItems, sortValue) { _, pos ->
                value = pos
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED)
        //dialog.window?.setBackgroundDrawable(ColorDrawable(0xFF000000.toInt()))
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun applySorting(sortValue: Int) {
        // Determine the sorting order based on sortValue
        val sortingOrder = when (sortValue) {
            0 -> MediaStore.Video.Media.DATE_ADDED + " DESC"
            1 -> MediaStore.Video.Media.DATE_ADDED
            2 -> MediaStore.Video.Media.TITLE
            3 -> MediaStore.Video.Media.TITLE + " DESC"
            4 -> MediaStore.Video.Media.SIZE
            5 -> MediaStore.Video.Media.SIZE + " DESC"
            else -> MediaStore.Video.Media.DATE_ADDED // Default to "Latest"
        }

        val sortedVideoList = when (sortingOrder) {
            MediaStore.Video.Media.DATE_ADDED + " DESC" -> {
                MainActivity.videoList.sortedByDescending { it.dateAdded }
            }
            MediaStore.Video.Media.DATE_ADDED -> {
                MainActivity.videoList.sortedBy { it.dateAdded }
            }
            MediaStore.Video.Media.TITLE -> {
                MainActivity.videoList.sortedBy { it.title }
            }
            MediaStore.Video.Media.TITLE + " DESC" -> {
                MainActivity.videoList.sortedByDescending { it.title }
            }
            MediaStore.Video.Media.SIZE -> {
                MainActivity.videoList.sortedBy { it.size }
            }
            MediaStore.Video.Media.SIZE + " DESC" -> {
                MainActivity.videoList.sortedByDescending { it.size }
            }
            else -> {
                MainActivity.videoList.sortedByDescending { it.dateAdded } // Default sorting order
            }
        }

        // Update the RecyclerView with the sorted list
        val sortedVideoArrayList = ArrayList(sortedVideoList)

        // Update the RecyclerView with the sorted ArrayList
        adapter.updateList(sortedVideoArrayList)
        adapter.notifyDataSetChanged()

    }
    class VideoComparator : Comparator<Video> {
        override fun compare(video1: Video, video2: Video): Int {
            // Compare videos based on their size
            return video1.size.compareTo(video2.size)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(PlayerActivity.position != -1) binding.nowplayingBtn.visibility = View.VISIBLE
        if(MainActivity.dataChanged) adapter.notifyDataSetChanged()
        MainActivity.dataChanged = false


    }



}