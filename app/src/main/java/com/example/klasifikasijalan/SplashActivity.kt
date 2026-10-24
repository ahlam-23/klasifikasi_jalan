package com.example.klasifikasijalan

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.klasifikasijalan.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UiAnimations.fadeInScale(binding.imgLogo)
        UiAnimations.fadeInUp(binding.tvAppName, 350)
        UiAnimations.fadeInUp(binding.tvTagline, 500)
        UiAnimations.fadeInUp(binding.loadingContainer, 650)

        binding.glowCircle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_soft))

        binding.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_in)
            finish()
        }, 2200)
    }
}
