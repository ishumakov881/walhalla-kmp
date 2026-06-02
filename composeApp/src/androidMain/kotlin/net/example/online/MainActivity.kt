package net.example.online

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import dev.walhalla.kmp.device.DeviceInfo
import net.example.online.push.PushLaunchIntentHandler


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DeviceInfo.initialize(this)
        ContextHelper.context = this@MainActivity

        //if (intent.isOpenedFromNotification()) {
        val extras = intent.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                Log.d("IntentExtras", "$key = $value")
            }
        } else {
            Log.d("IntentExtras", "No extras")
        }
        //}
        PushLaunchIntentHandler.handle(intent, fromNewIntent = false)
        setContent {
            App()
        }
        NotifierManager.onCreateOrOnNewIntent(intent)
    }

    //активити пересоздана с singleTop (клик по пушу в открытом приложении)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        //ignore ... PushLaunchIntentHandler.handle(intent, fromNewIntent = true)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }
}


@Preview
@Composable
fun AppAndroidPreview() {
    App()
}