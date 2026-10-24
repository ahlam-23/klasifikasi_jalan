package com.example.klasifikasijalan

import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object UiAnimations {

    fun staggerFadeUp(container: ViewGroup, delayStepMs: Long = 70L) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = 48f
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setStartDelay(i * delayStepMs)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun fadeInScale(view: View, delayMs: Long = 0L) {
        view.alpha = 0f
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delayMs)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    fun fadeInUp(view: View, delayMs: Long = 0L) {
        view.alpha = 0f
        view.translationY = 32f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun reveal(view: View) {
        if (view.visibility != View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
        }
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun pulse(view: View) {
        view.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(600)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .start()
            }
            .start()
    }
}
