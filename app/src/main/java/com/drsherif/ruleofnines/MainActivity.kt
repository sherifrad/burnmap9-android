package com.drsherif.ruleofnines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.drsherif.ruleofnines.ui.BurnScreen
import com.drsherif.ruleofnines.ui.theme.RuleOfNinesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RuleOfNinesTheme { BurnScreen() } }
    }
}
