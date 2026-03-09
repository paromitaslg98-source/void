package com.voidlauncher.app.ui.navigation

import android.animation.TimeInterpolator
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

object NavTransitionPolicy {

    private const val DEFAULT_DURATION_MS = 300

    enum class TransitionLanguage {
        PEER,
        HIERARCHICAL
    }

    fun Fragment.applyDestinationTransitions(language: TransitionLanguage, forward: Boolean = true) {
        val entering = buildTransition(language, forward)
        val returning = buildTransition(language, !forward)

        enterTransition = entering
        returnTransition = returning
        exitTransition = entering
        reenterTransition = returning
    }

    fun Fragment.applyExitFor(language: TransitionLanguage, forward: Boolean = true) {
        exitTransition = buildTransition(language, forward)
        reenterTransition = buildTransition(language, !forward)
    }

    private fun Fragment.buildTransition(language: TransitionLanguage, forward: Boolean) = when (language) {
        TransitionLanguage.PEER -> MaterialFadeThrough().configureMotion(this)
        TransitionLanguage.HIERARCHICAL -> MaterialSharedAxis(MaterialSharedAxis.Y, forward).configureMotion(this)
    }

    private fun <T : android.transition.Visibility> T.configureMotion(fragment: Fragment): T {
        val context = fragment.context ?: return this
        duration = MotionUtils.resolveThemeDuration(
            context,
            com.google.android.material.R.attr.motionDurationMedium4,
            DEFAULT_DURATION_MS
        ).toLong().coerceIn(250L, 400L)

        interpolator = MotionUtils.resolveThemeInterpolator(
            context,
            com.google.android.material.R.attr.motionEasingStandard,
            FastOutSlowInInterpolator() as TimeInterpolator
        )
        return this
    }
}
