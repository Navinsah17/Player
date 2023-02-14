package com.example.vplayer

import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vplayer.databinding.ActivityPlayerBinding
import com.example.vplayer.databinding.FeaturesBinding
import com.example.vplayer.dataclass.Video
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import kotlin.collections.ArrayList

class PlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable
    private var isSubtitle: Boolean = true

    companion object{
        lateinit var player : SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var repeat : Boolean = false
        private var isFull : Boolean = false
        private var isLock: Boolean = false
        lateinit var trackSelector : DefaultTrackSelector
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)


        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)
        //full screen------------------------------------
//        WindowCompat.setDecorFitsSystemWindows(window,false)
//        WindowInsetsControllerCompat(window,binding.root).let { controller ->
//            controller.hide(WindowInsetsCompat.Type.systemBars())
//            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        }
        //-----------------------------------------------------
        initializedLayout()
        initializedBinding()

    }

    private fun initializedLayout(){
        when(intent.getStringExtra("class")){
            "AllVideos" ->{
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPLayer()
            }
            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FolderAcivity.currentFolderVideos)
                createPLayer()
            }
        }
        if (repeat) binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)

    }
    private fun initializedBinding() {

        binding.backBtn.setOnClickListener {
            finish()
        }
        binding.playBtn.setOnClickListener {
            if (player.isPlaying) pauseVideo()
            else playVideo()
        }
        binding.nxtBtn.setOnClickListener {
            nextPrevVideo()
        }
        binding.prevBtn.setOnClickListener {
            nextPrevVideo(isNext = false)
        }
        binding.repeatBtn.setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }
        binding.fullScreenBtn.setOnClickListener {
            if (isFull) {
                isFull = false
                playInFullScreen(false)
            } else {
                isFull = true
                playInFullScreen(true)
            }
        }
        binding.lockBtn.setOnClickListener {
            if (!isLock) {
                isLock = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockBtn.setImageResource(R.drawable.baseline_lock_24)

            } else {
                isLock = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockBtn.setImageResource(R.drawable.lock_open_24)

            }
        }
        binding.moreBtn.setOnClickListener {
            pauseVideo()
            val customDialog = LayoutInflater.from(this).inflate(R.layout.features,binding.root,false)
            val bindingF = FeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this).setView(customDialog).setOnCancelListener {
                playVideo()
            }.setBackground(ColorDrawable(0x803700B3.toInt())).create()
            dialog.show()
//-------------------------------------------------------------------
            bindingF.audioTrack.setOnClickListener {
                dialog.dismiss()
                playVideo()

            val audioTrack = ArrayList<String>()
            for(i in 0 until player.currentTrackGroups.length){
                if (player.currentTrackGroups.get(i).getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT ){
                    audioTrack.add(Locale(player.currentTrackGroups.get(i).getFormat(0).language.toString()).displayLanguage)
                }
            }
                val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))

                MaterialAlertDialogBuilder(this,R.style.alertDialog)
                    .setTitle("Select Language")
                    .setOnCancelListener {
                    playVideo()
                }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks){_,position ->
                    Toast.makeText(this,audioTrack[position] + "Selected ", Toast.LENGTH_SHORT).show()
                    trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(audioTrack[position]))

                }
                    .create()
                    .show()
            }
            //---------------------------------------------------------------------------------------------
            bindingF.subtitleBtn.setOnClickListener {
                if (isSubtitle){
                    trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                        C.TRACK_TYPE_VIDEO, true
                    ).build()
                    Toast.makeText(this,"Subtitles Off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                }else{
                    trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                        C.TRACK_TYPE_VIDEO, false
                    ).build()
                    Toast.makeText(this,"Subtitles On", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
            }

        }
    }
    private fun createPLayer(){
        try {
            player.release()
        }catch (e: Exception){}
        trackSelector = DefaultTrackSelector(this)
        binding.videoTitle.text = playerList[position].title
        binding.videoTitle.isSelected = true
        player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        //player.play()
        playVideo()
        player.addListener(object : Player.Listener{
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })
        playInFullScreen(enable = isFull)
        setVisibility()

    }
    private fun playVideo(){
        binding.playBtn.setImageResource(R.drawable.baseline_pause_24)
        player.play()
    }
    private fun pauseVideo(){
        binding.playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
        player.pause()
    }
    private fun nextPrevVideo(isNext: Boolean = true){
        if (isNext) setPosition()
        else setPosition(isIncreement = false)
        createPLayer()
    }

    private fun setPosition(isIncreement: Boolean = true){
        if (!repeat){
            if (isIncreement){
                if (playerList.size -1 == position)
                    position = 0
                else ++position
            }else{
                if (position  == 0)
                    position = playerList.size -1
                else --position
            }
        }
    }

    private fun playInFullScreen(enable: Boolean){
        if (enable){
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            binding.fullScreenBtn.setImageResource(R.drawable.baseline_fullscreen_exit_24)
        }else{
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.fullScreenBtn.setImageResource(R.drawable.baseline_fullscreen_24)
        }

    }
    private fun setVisibility(){
        runnable = Runnable {
            if (binding.playerView.isControllerVisible){
                hideandShow(View.VISIBLE)
            }else{
                hideandShow(View.INVISIBLE)
            }///----------------------------------------------handle visibility
            Handler(Looper.getMainLooper()).postDelayed(runnable,300)

        }
        Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }

    private fun hideandShow(visibility : Int ){
        binding.topcontroller.visibility = visibility
        binding.bottomcontroller.visibility = visibility
        binding.playBtn.visibility = visibility
        if (isLock)
            binding.lockBtn.visibility = View.VISIBLE
        else binding.lockBtn.visibility = visibility
    }
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}
