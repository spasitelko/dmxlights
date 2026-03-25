package com.dmxlights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dmxlights.ui.navigation.NavGraph
import com.dmxlights.ui.theme.DmxLightsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DmxLightsTheme {
                NavGraph()
            }
        }
    }
}
