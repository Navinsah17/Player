package com.example.vplayer.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.vplayer.FolderAcivity
import com.example.vplayer.MainActivity
import com.example.vplayer.PlayerActivity
import com.example.vplayer.R
import com.example.vplayer.databinding.DetailsBinding
import com.example.vplayer.databinding.FeaturesBinding
import com.example.vplayer.databinding.VideoViewBinding
import com.example.vplayer.dataclass.Video
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class VideoAdapter(private val context: Context, var videoList: ArrayList<Video>,private val isFolder: Boolean = false ) : RecyclerView.Adapter<VideoAdapter.MyHolder>(){
    class MyHolder(binding : VideoViewBinding): RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val folder = binding.folderName
        val duartion = binding.duration
        val image = binding.videoIMG
        val root = binding.root



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.folder.text = videoList[position].folderName
        holder.duartion.text = DateUtils.formatElapsedTime(videoList[position].duration/1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.all_videos_icon).centerCrop())
            .into(holder.image)
        holder.root.setOnClickListener {

            when{
                videoList[position].id == PlayerActivity.nowlayingId -> {
                    sendIntent(pos = position,ref = "NowPlaying")
                }

                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(pos = position,ref = "FolderActivity")
                }
                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(pos = position,ref = "SearchedVideos")
                }
                else ->{
                    PlayerActivity.pipStatus = 3
                    sendIntent(pos = position, ref = "AllVideos")
                }
            }
        }
        holder.root.setOnLongClickListener{

            val customDialog = LayoutInflater.from(context).inflate(R.layout.more_features,holder.root,false)
            val bindingF = com.example.vplayer.databinding.MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
//                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialog.show()

            bindingF.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type ="video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM,Uri.parse(videoList[position].path))
                ContextCompat.startActivity(context,Intent.createChooser(shareIntent,"sharing video files"),null)
            }

            bindingF.renameBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val customDialogRF = LayoutInflater.from(context).inflate(R.layout.renameview,holder.root,false)
                val bindingRF = com.example.vplayer.databinding.RenameviewBinding.bind(customDialogRF)
                val dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
//                .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setCancelable(false)
                    .setPositiveButton("Rename"){self,_ ->
                        val currFile = File(videoList[position].path)
                        val newName = bindingRF.renameView.text
                        if(newName != null && currFile.exists() && newName.toString().isNotEmpty()){
                            val newFile = File(currFile.parentFile,newName.toString()+"."+currFile.extension)
                            if(currFile.renameTo(newFile)){
                                MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()),
                                    arrayOf("video/*"),null)
                                when {
                                    MainActivity.search -> {
                                        MainActivity.searchList[position].title = newName.toString()
                                        MainActivity.searchList[position].path = newFile.path
                                        MainActivity.searchList[position].artUri = Uri.fromFile(newFile)
                                        notifyItemChanged(position)
                                    }
                                    isFolder -> {
                                        FolderAcivity.currentFolderVideos[position].title = newName.toString()
                                        FolderAcivity.currentFolderVideos[position].path = newFile.path
                                        FolderAcivity.currentFolderVideos[position].artUri = Uri.fromFile(newFile)
                                        notifyItemChanged(position)
                                        MainActivity.dataChanged = true
                                    }
                                    else -> {
                                        MainActivity.videoList[position].title = newName.toString()
                                        MainActivity.videoList[position].path = newFile.path
                                        MainActivity.videoList[position].artUri = Uri.fromFile(newFile)
                                        notifyItemChanged(position)

                                    }
                                }
                            }else{
                                Toast.makeText(context,"Permission Denied",Toast.LENGTH_SHORT).show()
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel"){self,_ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
                bindingRF.renameView.text = SpannableStringBuilder(videoList[position].title)
                dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(context, R.attr.themeColor, Color.BLUE)
                )
                dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                    MaterialColors.getColor(context, R.attr.themeColor, Color.BLUE)
                )
            }

            bindingF.deleteBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()


                val dialogDF = MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Video")
                    .setMessage(videoList[position].title)
//                  .setBackground(ColorDrawable(0x803700B3.toInt()))
                    //.setCancelable(false)
                    .setPositiveButton("Yes"){self,_ ->
                        val file = File(videoList[position].path)
                        if(file.exists() && file.delete()){
                            MediaScannerConnection.scanFile(context, arrayOf(file.path),
                                arrayOf("video/*"),null)
                            when{
                                MainActivity.search -> {
                                    MainActivity.dataChanged = true
                                    videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                isFolder -> {
                                    MainActivity.dataChanged = true
                                    FolderAcivity.currentFolderVideos.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                else -> {
                                    MainActivity.videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                            }
                        }else{
                            Toast.makeText(context,"permission Denied", Toast.LENGTH_SHORT).show()

                        }
                        self.dismiss()
                    }
                    .setNegativeButton("No"){self,_ ->
                        self.dismiss()
                    }
                    .create()
                dialogDF.show()

                dialogDF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(context, R.attr.themeColor, Color.BLUE)
                )
                dialogDF.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                    MaterialColors.getColor(context, R.attr.themeColor, Color.BLUE)
                )
            }

            bindingF.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF = LayoutInflater.from(context).inflate(R.layout.details, holder.root, false)
                val bindingIF = DetailsBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("Ok"){self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()
                val detailsText = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }.append(videoList[position].title)
                    .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(videoList[position].duration/1000))
                    .bold { append("\n\nFile Size: ") }.append(Formatter.formatShortFileSize(context, videoList[position].size.toLong()))
                    .bold { append("\n\nLocation: ") }.append(videoList[position].path)


                bindingIF.detailTV.text = detailsText
                dialogIF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(context, R.attr.themeColor, Color.RED)
                )
            }

            return@setOnLongClickListener true
        }
    }
    private fun sendIntent(pos:Int , ref: String){
        PlayerActivity.position = pos
        val intent =  Intent(context,PlayerActivity::class.java)
        intent.putExtra("class",ref)
        ContextCompat.startActivity(context,intent,null)
    }
     fun updateList(searchList : ArrayList<Video>){
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }
    private fun requestPermissionR(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if(!Environment.isExternalStorageManager()){
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                    ContextCompat.startActivity(context, intent, null)
                }
            }
    }
}