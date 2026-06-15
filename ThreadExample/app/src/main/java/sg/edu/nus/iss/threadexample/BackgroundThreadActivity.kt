package sg.edu.nus.iss.threadexample

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class BackgroundThreadActivity : AppCompatActivity() {

    private lateinit var tvMainThreadInfo: TextView
    private lateinit var tvWorkerThreadInfo: TextView
    private lateinit var progressBarTask: ProgressBar
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvTaskStatus: TextView
    private lateinit var btnStartTask: MaterialButton
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_thread)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Background Thread"
        }

        tvMainThreadInfo = findViewById(R.id.tvMainThreadInfo)
        tvWorkerThreadInfo = findViewById(R.id.tvWorkerThreadInfo)
        progressBarTask = findViewById(R.id.progressBarTask)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvTaskStatus = findViewById(R.id.tvTaskStatus)
        btnStartTask = findViewById(R.id.btnStartTask)
        tvLog = findViewById(R.id.tvLog)

        val mainThread = Thread.currentThread()
        tvMainThreadInfo.text = "Name: ${mainThread.name}\nID:   ${mainThread.id}"

        btnStartTask.setOnClickListener { startBackgroundTask() }
    }

    private fun startBackgroundTask() {
        btnStartTask.isEnabled = false
        progressBarTask.progress = 0
        tvProgressPercent.text = "0%"
        tvLog.text = ""
        tvTaskStatus.text = "Starting..."

        val mainThreadId = Thread.currentThread().id
        log("[Main Thread #$mainThreadId] Launching worker thread...")

        Thread {
            val workerThread = Thread.currentThread()
            val workerThreadId = workerThread.id

            // runOnUiThread posts a Runnable to the main thread's MessageQueue.
            // Everything inside the lambda executes on the main thread.
            runOnUiThread {
                tvWorkerThreadInfo.text = "Name: ${workerThread.name}\nID:   $workerThreadId"
                tvTaskStatus.text = "Worker thread running..."
                log("[Worker #$workerThreadId] Started — posting UI updates via runOnUiThread()")
            }

            for (step in 1..10) {
                Thread.sleep(400)
                val progress = step * 10

                runOnUiThread {
                    progressBarTask.progress = progress
                    tvProgressPercent.text = "$progress%"
                    log("[Worker #$workerThreadId -> Main #${Thread.currentThread().id}] step $step posted")
                }
            }

            runOnUiThread {
                tvTaskStatus.text = "Task complete! The UI stayed responsive throughout."
                log("[Main Thread #${Thread.currentThread().id}] All steps done — UI updated")
                btnStartTask.isEnabled = true
            }
        }.apply { name = "WorkerThread-1" }.start()
    }

    private fun log(message: String) {
        val current = tvLog.text.toString()
        tvLog.text = if (current.isEmpty()) message else "$current\n$message"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
