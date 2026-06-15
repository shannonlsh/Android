package com.example.camera_example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.camera_example.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var isVideoMode = false
    private var isRecording = false
    private var recordingSeconds = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            recordingSeconds++
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            binding.tvTimer.text = "%02d:%02d".format(mins, secs)
            timerHandler.postDelayed(this, 1000)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera and audio permissions are required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        binding.btnCapture.setOnClickListener {
            if (isVideoMode) toggleVideoRecording() else takePhoto()
        }

        binding.btnFlipCamera.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA
            startCamera()
        }

        binding.imgThumbnail.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnModePhoto.setOnClickListener { setMode(false) }
        binding.btnModeVideo.setOnClickListener { setMode(true) }
    }

    private fun setMode(video: Boolean) {
        isVideoMode = video
        binding.btnModePhoto.setTextColor(
            if (!video) getColor(android.R.color.white) else getColor(android.R.color.darker_gray)
        )
        binding.btnModeVideo.setTextColor(
            if (video) getColor(android.R.color.white) else getColor(android.R.color.darker_gray)
        )
        if (video) {
            binding.btnCapture.setImageResource(R.drawable.ic_shutter_record)
        } else {
            binding.btnCapture.setImageResource(R.drawable.ic_shutter)
        }
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                if (isVideoMode) {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                    videoCapture = VideoCapture.withOutput(recorder)
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                } else {
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val photoFile = createFile("IMG", ".jpg", picturesDir())

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    updateThumbnail(photoFile)
                    Toast.makeText(this@MainActivity, "Photo saved", Toast.LENGTH_SHORT).show()
                }
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Photo failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleVideoRecording() {
        if (isRecording) {
            recording?.stop()
            recording = null
        } else {
            val videoFile = createFile("VID", ".mp4", videosDir())
            val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

            recording = videoCapture!!.output
                .prepareRecording(this, fileOutputOptions)
                .apply { withAudioEnabled() }
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            isRecording = true
                            binding.tvRecording.visibility = View.VISIBLE
                            binding.tvTimer.visibility = View.VISIBLE
                            recordingSeconds = 0
                            timerHandler.post(timerRunnable)
                            binding.btnCapture.setImageResource(R.drawable.ic_shutter_stop)
                        }
                        is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            binding.tvRecording.visibility = View.GONE
                            binding.tvTimer.visibility = View.GONE
                            timerHandler.removeCallbacks(timerRunnable)
                            binding.btnCapture.setImageResource(R.drawable.ic_shutter_record)
                            if (!event.hasError()) {
                                updateThumbnail(videoFile)
                                Toast.makeText(this, "Video saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Video error: ${event.error}", Toast.LENGTH_SHORT).show()
                                videoFile.delete()
                            }
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun updateThumbnail(file: File) {
        Glide.with(this).load(file).centerCrop().into(binding.imgThumbnail)
    }

    private fun picturesDir(): File {
        return File(getExternalFilesDir(null), "Pictures").also { it.mkdirs() }
    }

    private fun videosDir(): File {
        return File(getExternalFilesDir(null), "Movies").also { it.mkdirs() }
    }

    private fun createFile(prefix: String, suffix: String, dir: File): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "${prefix}_$timestamp$suffix")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        // Refresh thumbnail with latest media
        val latest = (picturesDir().listFiles() ?: emptyArray<File>())
            .plus(videosDir().listFiles() ?: emptyArray<File>())
            .maxByOrNull { it.lastModified() }
        if (latest != null) {
            Glide.with(this).load(latest).centerCrop().into(binding.imgThumbnail)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        timerHandler.removeCallbacks(timerRunnable)
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
