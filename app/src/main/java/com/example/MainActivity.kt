package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.AlarmNavHost
import com.example.ui.theme.CalmAlarmTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { CalmAlarmContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmAlarmTheme {
                AlarmNavHost(container = container)
            }
        }
    }
}
