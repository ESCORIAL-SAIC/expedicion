package com.expedicion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.expedicion.app.ui.navigation.ExpedicionNavGraph
import com.expedicion.app.ui.theme.ExpedicionTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpedicionTheme {
                ExpedicionNavGraph()
            }
        }
    }
}
