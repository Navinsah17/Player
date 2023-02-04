package com.example.vplayer

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vplayer.databinding.ActivityPlayerBinding
import com.example.vplayer.dataclass.Video
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

class PlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerBinding

    companion object{
        lateinit var player : SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var repeat : Boolean = false
        private var isFull : Boolean = false
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
    private fun initializedBinding(){

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
            if (repeat){
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            }else{
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }
        binding.fullScreenBtn.setOnClickListener {
            if (isFull) {
                isFull = false
                playInFullScreen(false)
            }else{
                isFull = true
                playInFullScreen(true)
            }
        }
    }

    private fun createPLayer(){
        try {
            player.release()
        }catch (e: Exception){}
        binding.videoTitle.text = playerList[position].title
        binding.videoTitle.isSelected = true
        player = SimpleExoPlayer.Builder(this).build()
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
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}
