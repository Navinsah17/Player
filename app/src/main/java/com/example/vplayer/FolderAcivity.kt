package com.example.vplayer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vplayer.adapter.FolderAdapter
import com.example.vplayer.adapter.VideoAdapter
import com.example.vplayer.databinding.ActivityFolderAcivityBinding
import com.example.vplayer.dataclass.Folder
import com.example.vplayer.dataclass.Video
import java.io.File

class FolderAcivity : AppCompatActivity() {

    companion object{

        lateinit var currentFolderVideos: ArrayList<Video>


    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFolderAcivityBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueeNav)

        setContentView(binding.root)


        val position = intent.getIntExtra("position",0)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName
        currentFolderVideos = getAllVideo(MainActivity.folderList[position].id)
        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.layoutManager = LinearLayoutManager(this@FolderAcivity)
        binding.videoRVFA.adapter = VideoAdapter(this@FolderAcivity, currentFolderVideos,true)
        binding.totalVdsFA.text ="Total Videos: ${currentFolderVideos.size}"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
    @SuppressLint("Recycle", "Range", "SuspiciousIndentation")
    private fun getAllVideo(folderId: String): ArrayList<Video>{
        val tempList = ArrayList<Video>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "
        //val tempFolderList = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.SIZE, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.BUCKET_ID)
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(folderId),
            MainActivity.sortList[MainActivity.sortValue])
        if(cursor != null)
            if(cursor.moveToNext())
                do {
                    //checking null safety with ?: operator
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))?:"Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))?:"Unknown"
                    val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))?:"Internal Storage"
                    //val folderIdC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))?:"Unknown"
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))?:"0"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))?:"Unknown"
                    val durationC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))?.toLong()?:0L

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val video = Video(title = titleC, id = idC, folderName = folderC, duration = durationC, size = sizeC,
                            path = pathC, artUri = artUriC)
                        if(file.exists()) tempList.add(video)

                    }catch (e:Exception){}
                }while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }

}