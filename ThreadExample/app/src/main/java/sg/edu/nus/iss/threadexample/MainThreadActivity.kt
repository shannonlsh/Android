package sg.edu.nus.iss.threadexample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class MainThreadActivity : AppCompatActivity() {

    private lateinit var tvThreadName: TextView
    private lateinit var tvThreadId: TextView
    private lateinit var tvIsMainThread: TextView
    private lateinit var progressBarHandler: ProgressBar
    private lateinit var tvCounter: TextView
    private lateinit var btnStartCounter: MaterialButton
    private lateinit var btnStopCounter: MaterialButton
    private lateinit var btnBlockThread: MaterialButton
    private lateinit var tvBlockStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var counter = 0
    private var isCounterRunning = false

    // Recurring Runnable — re-schedules itself every 100 ms via the main thread's Looper.
    // Because it runs on the main thread, any UI update inside is safe without extra synchronization.
    private val counterRunnable = object : Runnable {
        override fun run() {
            if (!isCounterRunning) return
            counter++
            tvCounter.text = "Counter: $counter"
            progressBarHandler.progress = counter % 101
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_thread)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Main (UI) Thread"
        }

        tvThreadName = findViewById(R.id.tvThreadName)
        tvThreadId = findViewById(R.id.tvThreadId)
        tvIsMainThread = findViewById(R.id.tvIsMainThread)
        progressBarHandler = findViewById(R.id.progressBarHandler)
        tvCounter = findViewById(R.id.tvCounter)
        btnStartCounter = findViewById(R.id.btnStartCounter)
        btnStopCounter = findViewById(R.id.btnStopCounter)
        btnBlockThread = findViewById(R.id.btnBlockThread)
        tvBlockStatus = findViewById(R.id.tvBlockStatus)

        val thread = Thread.currentThread()
        val isMain = Looper.myLooper() == Looper.getMainLooper()
        tvThreadName.text = "Thread Name: ${thread.name}"
        tvThreadId.text = "Thread ID:   ${thread.id}"
        tvIsMainThread.text = "Is Main Thread: $isMain"

        btnStartCounter.setOnClickListener {
            isCounterRunning = true
            handler.post(counterRunnable)
            btnStartCounter.isEnabled = false
            btnStopCounter.isEnabled = true
        }

        btnStopCounter.setOnClickListener {
            isCounterRunning = false
            handler.removeCallbacks(counterRunnable)
            btnStartCounter.isEnabled = true
            btnStopCounter.isEnabled = false
        }

        btnBlockThread.setOnClickListener {
            btnBlockThread.isEnabled = false
            // Setting text here will NOT be visible — the next frame cannot be drawn
            // until the main thread is free again, which is after the sleep finishes.
            tvBlockStatus.text = "Freezing... (you will not see this message appear)"
            Thread.sleep(3_000)
            // Only this message becomes visible because it is set after the thread unblocks.
            tvBlockStatus.text = "Unblocked! Notice: the counter paused, the spinner froze, and even the 'Freezing…' message never appeared — the main thread had no chance to redraw."
            btnBlockThread.isEnabled = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        isCounterRunning = false
        handler.removeCallbacks(counterRunnable)
    }
}
