package sg.edu.nus.iss.threadexample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        findViewById<MaterialButton>(R.id.btnGoMainThread).setOnClickListener {
            startActivity(Intent(this@MainActivity, MainThreadActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnGoBackgroundThread).setOnClickListener {
            startActivity(Intent(this@MainActivity, BackgroundThreadActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnGoThreadSafety).setOnClickListener {
            startActivity(Intent(this@MainActivity, ThreadSafetyActivity::class.java))
        }
    }
}
