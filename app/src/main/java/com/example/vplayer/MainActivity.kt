package com.example.vplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.vplayer.databinding.ActivityMainBinding
import com.example.vplayer.databinding.FeaturesBinding
import com.example.vplayer.dataclass.Folder
import com.example.vplayer.dataclass.Video
import com.example.vplayer.fragment.FolderFragment
import com.example.vplayer.fragment.VideoFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var toggle: ActionBarDrawerToggle
    private var runnable: Runnable? = null
    private lateinit var currentFragment : Fragment


    companion object{
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
        lateinit var searchList: ArrayList<Video>
        var search: Boolean = false
        var sortValue: Int = 0
        var dataChanged : Boolean = false
        var adapterChanged : Boolean = false


        val sortList = arrayOf(
            MediaStore.Video.Media.DATE_ADDED + " DESC",
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.TITLE + " DESC",
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE + " DESC"
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setTheme(R.style.coolBlueeNav)
        setContentView(binding.root)



        toggle = ActionBarDrawerToggle(this,binding.root,R.string.open,R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (requestRuntimePermission()){
            folderList = ArrayList()
            videoList = getAllVideo()
            setFragment(VideoFragment())


//            runnable = Runnable {
//
//                if(dataChanged){
//                    videoList = getAllVideo()
//                    dataChanged = false
//                    adapterChanged = true
//
//                }
//
//                ///----------------------------------------------handle visibility
//                Handler(Looper.getMainLooper()).postDelayed(runnable!!,200)
//
//                }
//                Handler(Looper.getMainLooper()).postDelayed(runnable!!,0)

        }else{
            folderList = ArrayList()
            videoList = ArrayList()
        }


        binding.bottomNav.setOnItemSelectedListener {
            //if(dataChanged) videoList = getAllVideo()
            when (it.itemId) {
                R.id.videoView -> setFragment(VideoFragment())
                R.id.foldersView -> setFragment(FolderFragment())
            }
            return@setOnItemSelectedListener true
        }

        binding.navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navYoutube -> toast("youtube")
                R.id.navUrl-> toast("online")
                R.id.themesNav -> toast("themes")


                R.id.sortOrderNav -> {
                    val menuItems = arrayOf(
                        "Latest",
                        "Oldest",
                        "Name(A to Z)",
                        "Name(Z to A)",
                        "File Size(Smallest)",
                        "File Size(Largest)"
                    )

                    var value = sortValue
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle("Sort By")
                        .setPositiveButton("OK") { _, _ ->
                            val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE).edit()
                            sortEditor.putInt("sortValue", value)
                            sortEditor.apply()

                            //for restarting app
                            finish()
                            startActivity(intent)
                        }

                        .setSingleChoiceItems(menuItems, sortValue) { _, pos ->
                            value = pos
                        }

                        .create()
                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.RED)
                }

                R.id.aboutNav -> toast("about")
                R.id.exitNav -> exitProcess(1)
            }

            return@setNavigationItemSelectedListener true
        }
    }

    private fun setFragment(fragment: Fragment){
        currentFragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameFL, fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }

    private fun requestRuntimePermission(): Boolean{

        //requesting storage permission for only devices less than api 28
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            if(ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),13)
                return false
            }
        }else{
            //read external storage permission for devices higher than android 10 i.e. api 29
            if(ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),14)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 13) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
               folderList = ArrayList()
               videoList = getAllVideo()
                setFragment(VideoFragment())
            }
            else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
                .setAction("OK"){
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),13)
                }
                .show()
//                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
        }

        //for read external storage permission
        if(requestCode == 14) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
//                folderList = ArrayList()
//                videoList = getAllVideos(this)
                setFragment(VideoFragment())
            }
            else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
                .setAction("OK"){
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),14)
                }
                .show()
//            else
//                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE),14)
        }
    }

    private fun toast(message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }
    @SuppressLint("Recycle", "Range", "SuspiciousIndentation")
    private fun getAllVideo(): ArrayList<Video>{
        val sortEditor = getSharedPreferences("Sorting", MODE_PRIVATE)
        sortValue = sortEditor.getInt("sortValue",0)
        val tempList = ArrayList<Video>()
        val tempFolderList = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.SIZE, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.BUCKET_ID)
        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            sortList[sortValue])
        if(cursor != null)
            if(cursor.moveToNext())
                do {
                    //checking null safety with ?: operator
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))?:"Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))?:"Unknown"
                    val folderC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))?:"Internal Storage"
                    val folderIdC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))?:"Unknown"
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))?:"0"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))?:"Unknown"
                    val durationC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))?.toLong()?:0L

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val video = Video(title = titleC, id = idC, folderName = folderC, duration = durationC, size = sizeC,
                            path = pathC, artUri = artUriC)
                        if(file.exists()) tempList.add(video)


                        if(!tempFolderList.contains(folderC) && !folderC.contains("Internal Storage")){
                            tempFolderList.add(folderC)
                            folderList.add(Folder(id = folderIdC, folderName = folderC))
                        }


                    }catch (e:Exception){}
                }while (cursor.moveToNext())
        cursor?.close()
        return tempList
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable = null
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        (currentFragment as VideoFragment).adapter.onResult(requestCode, resultCode)
    }

}