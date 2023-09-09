package com.example.vplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vplayer.databinding.ActivityPlayerBinding
import com.example.vplayer.databinding.AudioBoosterBinding
import com.example.vplayer.databinding.FeaturesBinding
import com.example.vplayer.databinding.SpeedBinding
import com.example.vplayer.dataclass.Video
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener {
    lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable
    private var isSubtitle: Boolean = true
    private var moreTime: Int = 0;

    companion object{
        lateinit var player : SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        var repeat : Boolean = false
        private var isFull : Boolean = false
        private var isLock: Boolean = false
        var nowlayingId : String = ""
        private var speed: Float = 1.0f
        lateinit var trackSelector : DefaultTrackSelector
        lateinit var audioEnhancer: LoudnessEnhancer
        var timer: Timer? = null
        private var isSpeedChecked: Boolean = false
        var pipStatus: Int = 0
        private var audioManager: AudioManager? = null
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

        try{
            if(intent.data?.scheme.contentEquals("content")){

                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(intent.data!!, arrayOf(MediaStore.Video.Media.DATA),null,null,null)
                cursor?.let{
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(id = "", title =  file.name, duration = 0L , artUri = Uri.fromFile(file),path = path, size = "", folderName = "")
                    playerList.add(video)
                    cursor.close()

                }
                createPLayer()
                initializedBinding()
            }
            else{
                initializedLayout()
                initializedBinding()
            }
        }catch (e: Exception){
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
        }
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
            "SearchedVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPLayer()
            }
            "NowPlaying" ->{
                speed = 1.0f
                binding.videoTitle.text = playerList[position].title
                binding.videoTitle.isSelected = true
                binding.playerView.player = player
                playVideo()
                playInFullScreen(enable = isFull)
                setVisibility()
            }
        }
        if (repeat) binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)

    }
    private fun initializedBinding() {
        binding.orientationBtn.setOnClickListener {
            requestedOrientation = if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        binding.forward.setOnClickListener(DoubleClick(callback = object : DoubleClick.Callback{
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.forwardbtn.visibility = View.VISIBLE
                player.seekTo(player.currentPosition + 10000)
            }
        }))
        binding.rewind.setOnClickListener(DoubleClick(callback = object : DoubleClick.Callback{
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.rewindbtn.visibility = View.VISIBLE
                player.seekTo(player.currentPosition - 10000)
            }
        }))

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
//-----------------------------audio--------------------------------------
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
                    .setBackground(ColorDrawable(0xFF000000.toInt()))
                    .setItems(tempTracks){_,position ->
                    Toast.makeText(this,audioTrack[position] + "Selected ", Toast.LENGTH_SHORT).show()
                    trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setPreferredAudioLanguage(audioTrack[position]))

                }
                    .create()
                    .show()
            }
            //--------------------subtitle-------------------------------------------------------------------------
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
                dialog.dismiss()
                playVideo()
            }
            //---------------audio-----------------------------------
            bindingF.audioBooster.setOnClickListener {
                dialog.dismiss()
                val customDialogAB = LayoutInflater.from(this).inflate(R.layout.audio_booster,binding.root,false)
                val bindingAB = AudioBoosterBinding.bind(customDialogAB)
                val dialogAB = MaterialAlertDialogBuilder(this).setView(customDialogAB).setOnCancelListener {
                    playVideo()
                }.setPositiveButton("OK"){self, _ ->
                    audioEnhancer.setTargetGain(bindingAB.verticalBar.progress*100)
                    playVideo()
                    self.dismiss()
                }
                    .setBackground(ColorDrawable(0xFF000000.toInt()))
                    .create()

                dialogAB.show()
                bindingAB.verticalBar.progress = audioEnhancer.targetGain.toInt()/100
                bindingAB.progressText.text = "Audio Boost\n\n${audioEnhancer.targetGain.toInt()/10}"
                bindingAB.verticalBar.setOnProgressChangeListener {
                    bindingAB.progressText.text = "Audio Boost\n\n${it*10}"
                }
                playVideo()
            }
            //--------------speed------------------
            bindingF.speedbtn.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val customDialogS = LayoutInflater.from(this).inflate(R.layout.speed, binding.root, false)
                val bindingS = SpeedBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("OK"){self, _ ->
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0xFF000000.toInt()))
                    .create()
                dialogS.show()
                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }

            }
            //-------------sleep-----------
            bindingF.sleepbtn.setOnClickListener {
                dialog.dismiss()
                if (timer != null)  Toast.makeText(this,"Timer already running! \n Close App to Reset !",Toast.LENGTH_SHORT).show()

                else{
                    var sleepTime = 15
                    val customDialogS = LayoutInflater.from(this).inflate(R.layout.speed, binding.root, false)
                    val bindingS = SpeedBinding.bind(customDialogS)
                    val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                        .setCancelable(false)
                        .setPositiveButton("OK"){self, _ ->
                            timer = Timer()
                            val task = object :TimerTask(){
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }
                            timer!!.schedule(task, sleepTime *60 * 1000.toLong())
                            self.dismiss()
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0xFF000000.toInt()))
                        .create()
                    dialogS.show()
                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime > 15 ) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime < 120)sleepTime += 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }

            }
            //---------------------pipbtn-------------
            bindingF.pipbtn.setOnClickListener {
                val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName)==
                            AppOpsManager.MODE_ALLOWED
                } else { false }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if (status) {
                        this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dialog.dismiss()
                        binding.playerView.hideController()
                        playVideo()
                        pipStatus = 0
                    }
                    else{
                        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package:$packageName"))
                        startActivity(intent)
                    }

                }else{
                    Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    playVideo()
                }
            }
            //-----------------------------------------

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
        audioEnhancer = LoudnessEnhancer(player.audioSessionId)
        audioEnhancer.enabled = true

        nowlayingId = playerList[position].id

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

    private fun changeSpeed(isIncrement: Boolean){
        if(isIncrement){
            if(speed <= 2.9f){
                speed += 0.10f //speed = speed + 0.10f
            }
        }
        else{
            if(speed > 0.20f){
                speed -= 0.10f
            }
        }
        player.setPlaybackSpeed(speed)
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

    @SuppressLint("MissingSuperCall")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {

        if(pipStatus != 0){
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when(pipStatus){
                1 -> intent.putExtra("class","FolderActivity")
                2 -> intent.putExtra("class","SearchedVideos")
                3 -> intent.putExtra("class","AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()


    }

    private fun hideandShow(visibility : Int ){
        binding.topcontroller.visibility = visibility
        binding.bottomcontroller.visibility = visibility
        binding.playBtn.visibility = visibility
        if (isLock)
            binding.lockBtn.visibility = View.VISIBLE
        else binding.lockBtn.visibility = visibility
        binding.rewindbtn.visibility = View.GONE
        binding.forwardbtn.visibility = View.GONE

    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        audioManager?.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if(focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)
    }

}
