package com.sparesapp.register

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sparesapp.register.ui.nav.SparesNavGraph
import com.sparesapp.register.ui.theme.SparesRegisterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SparesApp
        setContent {
            SparesRegisterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SparesNavGraph(app)
                }
            }
        }
    }
}
