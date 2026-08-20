package com.mobileapp.xpensa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobileapp.xpensa.ui.PantryApp
import com.mobileapp.xpensa.ui.theme.XpensaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XpensaTheme {
                PantryApp()
            }
        }
    }
}
