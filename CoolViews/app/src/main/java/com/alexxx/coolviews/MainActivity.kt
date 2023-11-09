package com.alexxx.coolviews

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation.INFINITE
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val animator = ObjectAnimator.ofFloat(textView, View.ALPHA, 0f, 1f, 0f)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.repeatCount = INFINITE
        animator.duration = 3000
        animator.start()
    }
}