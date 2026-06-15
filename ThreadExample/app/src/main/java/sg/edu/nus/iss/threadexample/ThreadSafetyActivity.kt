package sg.edu.nus.iss.threadexample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.concurrent.atomic.AtomicInteger

class ThreadSafetyActivity : AppCompatActivity() {

    private lateinit var tvUnsafeResult: TextView
    private lateinit var tvSafeResult: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnUnsafe: MaterialButton
    private lateinit var btnSafe: MaterialButton

    // Plain Int — read-modify-write is NOT atomic, so concurrent ++ causes lost updates.
    private var unsafeCounter = 0

    // AtomicInteger — incrementAndGet() is a single hardware-level atomic operation.
    private val safeCounter = AtomicInteger(0)

    private companion object {
        const val THREAD_COUNT = 10
        const val INCREMENTS_PER_THREAD = 1_000
        const val EXPECTED = THREAD_COUNT * INCREMENTS_PER_THREAD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thread_safety)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Thread Safety"
        }

        tvUnsafeResult = findViewById(R.id.tvUnsafeResult)
        tvSafeResult = findViewById(R.id.tvSafeResult)
        tvLog = findViewById(R.id.tvLog)
        btnUnsafe = findViewById(R.id.btnUnsafe)
        btnSafe = findViewById(R.id.btnSafe)

        btnUnsafe.setOnClickListener { runUnsafeDemo() }
        btnSafe.setOnClickListener { runSafeDemo() }
    }

    private fun runUnsafeDemo() {
        btnUnsafe.isEnabled = false
        tvUnsafeResult.text = "Running $THREAD_COUNT threads..."
        log("--- Unsafe run started ---")
        log("$THREAD_COUNT threads each calling counter++ $INCREMENTS_PER_THREAD times")

        Thread {
            unsafeCounter = 0

            val threads = (1..THREAD_COUNT).map { n ->
                Thread {
                    repeat(INCREMENTS_PER_THREAD) {
                        unsafeCounter++ // READ → INCREMENT → WRITE: not atomic!
                    }
                }.apply { name = "unsafe-$n" }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val actual = unsafeCounter
            val lost = EXPECTED - actual

            runOnUiThread {
                if (actual == EXPECTED) {
                    tvUnsafeResult.text = "Expected: $EXPECTED\nActual:   $actual\nGot lucky this run — try again to see data loss"
                    log("Result: $actual/$EXPECTED (no loss this time — try again)")
                } else {
                    tvUnsafeResult.text = "Expected: $EXPECTED\nActual:   $actual\nLost $lost increments due to race condition!"
                    log("Result: $actual/$EXPECTED — $lost increments were LOST to race conditions")
                }
                btnUnsafe.isEnabled = true
            }
        }.start()
    }

    private fun runSafeDemo() {
        btnSafe.isEnabled = false
        tvSafeResult.text = "Running $THREAD_COUNT threads..."
        log("--- Safe run started ---")
        log("$THREAD_COUNT threads each calling safeCounter.incrementAndGet() $INCREMENTS_PER_THREAD times")

        Thread {
            safeCounter.set(0)

            val threads = (1..THREAD_COUNT).map { n ->
                Thread {
                    repeat(INCREMENTS_PER_THREAD) {
                        safeCounter.incrementAndGet() // Single atomic CAS instruction — always consistent
                    }
                }.apply { name = "safe-$n" }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val actual = safeCounter.get()

            runOnUiThread {
                tvSafeResult.text = "Expected: $EXPECTED\nActual:   $actual\nAlways correct — AtomicInteger guarantees atomicity"
                log("Result: $actual/$EXPECTED — no increments lost")
                btnSafe.isEnabled = true
            }
        }.start()
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
