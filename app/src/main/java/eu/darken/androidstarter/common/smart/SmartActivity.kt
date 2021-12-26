package eu.darken.androidstarter.common.smart

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.darken.androidstarter.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.androidstarter.common.debug.logging.log
import eu.darken.androidstarter.common.debug.logging.logTag

abstract class SmartActivity : AppCompatActivity() {
    internal val tag: String =
        logTag("Activity", this.javaClass.simpleName + "(" + Integer.toHexString(hashCode()) + ")")

    override fun onCreate(savedInstanceState: Bundle?) {
        log(tag, VERBOSE) { "onCreate(savedInstanceState=$savedInstanceState)" }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        log(tag, VERBOSE) { "onResume()" }
        super.onResume()
    }

    override fun onPause() {
        log(tag, VERBOSE) { "onPause()" }
        super.onPause()
    }

    override fun onDestroy() {
        log(tag, VERBOSE) { "onDestroy()" }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        log(tag, VERBOSE) { "onActivityResult(requestCode=$requestCode, resultCode=$resultCode, data=$data)" }
        super.onActivityResult(requestCode, resultCode, data)
    }

}