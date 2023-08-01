package com.example.vplayer.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.vplayer.MainActivity
import com.example.vplayer.PlayerActivity
import com.example.vplayer.R
import com.example.vplayer.databinding.DetailsBinding
import com.example.vplayer.databinding.FeaturesBinding
import com.example.vplayer.databinding.VideoViewBinding
import com.example.vplayer.dataclass.Video
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
}