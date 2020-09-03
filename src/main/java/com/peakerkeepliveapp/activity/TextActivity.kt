package com.peakerkeepliveapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.peakerkeepliveapp.R
import kotlinx.android.synthetic.main.activity_text.*

class TextActivity : AppCompatActivity() {
    private  val TAG = "TextActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        tv.setText("onCreate修改过的值")
        Log.d(TAG, "onCreate: ")
    }

    override fun onResume() {
        super.onResume()
        tv.setText("onResume修改过的值")
        Log.d(TAG, "onResume: ")
    }

    override fun onRestart() {
        super.onRestart()
        tv.setText("onRestart修改过的值")
        Log.d(TAG, "onRestart: ")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }
}