package com.voidlauncher.app.ui.navigation

import android.view.Gravity
import androidx.core.graphics.PathParser
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import android.view.animation.PathInterpolator

/**
 * Material Design 3 Expressive direction-aware transitions.
 *
 * Each gesture maps to a [Direction] that determines the slide axis and gravity:
 * - RIGHT  → content slides in from the right edge  (Notes)
 * - LEFT   → content slides in from the left edge   (Notifications)
 * - UP     → content slides in from the bottom edge  (App Drawer)
 * - FADE   → non-directional fade-through            (Settings, Home root)
 */
object NavTransitionPolicy {

    // ── MD3 Expressive timing ──────────────────────────────────────────────────
    private const val ENTER_DURATION_MS = 350L
    private const val EXIT_DURATION_MS  = 250L
    private const val FADE_DURATION_MS  = 300L

    // MD3 emphasized easing curves
    private val ENTER_INTERPOLATOR = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)
    private val EXIT_INTERPOLATOR  = PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f)
    private val FADE_INTERPOLATOR  = FastOutSlowInInterpolator()

    enum class Direction {
        LEFT, RIGHT, UP, FADE
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Called in the DESTINATION fragment's `onCreate()`.
     * Sets enter, return, exit, and reenter transitions so both forward and
     * backward navigation feels directional.
     */
    fun Fragment.applyDestinationTransitions(direction: Direction) {
        if (direction == Direction.FADE) {
            enterTransition  = buildFadeThrough()
            returnTransition = buildFadeThrough()
            exitTransition   = buildFadeThrough()
            reenterTransition = buildFadeThrough()
            return
        }

        enterTransition   = buildEnterTransition(direction)
        returnTransition  = buildReturnTransition(direction)
        exitTransition    = buildExitTransition(direction)
        reenterTransition = buildReenterTransition(direction)
    }

    /**
     * Called in the ORIGIN fragment right before `navigate()`.
     * Sets exit + reenter so the departing fragment slides out in the correct
     * direction and slides back in when the user returns.
     */
    fun Fragment.applyExitFor(direction: Direction) {
        if (direction == Direction.FADE) {
            exitTransition    = buildFadeThrough()
            reenterTransition = buildFadeThrough()
            return
        }

        exitTransition    = buildExitTransition(direction)
        reenterTransition = buildReenterTransition(direction)
    }

    // ── Transition builders ────────────────────────────────────────────────────

    /**
     * Enter: content slides IN from [direction] edge + fades in.
     * e.g. Direction.RIGHT → slides in from the right edge (Gravity.END).
     */
    private fun buildEnterTransition(direction: Direction): TransitionSet {
        val slideGravity = enterGravity(direction)
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Slide(slideGravity).apply {
                duration = ENTER_DURATION_MS
                interpolator = ENTER_INTERPOLATOR
            })
            addTransition(Fade(Fade.IN).apply {
                duration = ENTER_DURATION_MS
                interpolator = ENTER_INTERPOLATOR
            })
        }
    }

    /**
     * Return (pop back): content slides OUT toward [direction] edge + fades out.
     * This is the reverse of enter — same direction, opposite motion.
     */
    private fun buildReturnTransition(direction: Direction): TransitionSet {
        val slideGravity = enterGravity(direction)
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Slide(slideGravity).apply {
                duration = EXIT_DURATION_MS
                interpolator = EXIT_INTERPOLATOR
            })
            addTransition(Fade(Fade.OUT).apply {
                duration = EXIT_DURATION_MS
                interpolator = EXIT_INTERPOLATOR
            })
        }
    }

    /**
     * Exit (origin departing): content shifts slightly AWAY from [direction] + fades out.
     * Creates a subtle parallax effect — the origin moves a bit to "make room".
     */
    private fun buildExitTransition(direction: Direction): TransitionSet {
        val slideGravity = exitGravity(direction)
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Slide(slideGravity).apply {
                duration = EXIT_DURATION_MS
                interpolator = EXIT_INTERPOLATOR
            })
            addTransition(Fade(Fade.OUT).apply {
                duration = EXIT_DURATION_MS
                interpolator = EXIT_INTERPOLATOR
                startDelay = 50  // slight delay so slide leads
            })
        }
    }

    /**
     * Reenter (origin coming back): content slides back from opposite side + fades in.
     * Reverse of exit — origin returns to its natural position.
     */
    private fun buildReenterTransition(direction: Direction): TransitionSet {
        val slideGravity = exitGravity(direction)
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Slide(slideGravity).apply {
                duration = ENTER_DURATION_MS
                interpolator = ENTER_INTERPOLATOR
            })
            addTransition(Fade(Fade.IN).apply {
                duration = ENTER_DURATION_MS
                interpolator = ENTER_INTERPOLATOR
            })
        }
    }

    /**
     * Fade-through: sequential fade-out then fade-in.
     * Used for non-directional transitions (Settings, root Home).
     */
    private fun buildFadeThrough(): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            addTransition(Fade(Fade.OUT).apply {
                duration = FADE_DURATION_MS / 2
                interpolator = FADE_INTERPOLATOR
            })
            addTransition(Fade(Fade.IN).apply {
                duration = FADE_DURATION_MS / 2
                interpolator = FADE_INTERPOLATOR
            })
        }
    }

    // ── Gravity helpers ────────────────────────────────────────────────────────

    /** The edge the ENTERING content comes from. */
    private fun enterGravity(direction: Direction): Int = when (direction) {
        Direction.RIGHT -> Gravity.END
        Direction.LEFT  -> Gravity.START
        Direction.UP    -> Gravity.BOTTOM
        Direction.FADE  -> Gravity.BOTTOM // fallback, shouldn't be reached
    }

    /** The edge the EXITING (origin) content moves toward (opposite of enter). */
    private fun exitGravity(direction: Direction): Int = when (direction) {
        Direction.RIGHT -> Gravity.START
        Direction.LEFT  -> Gravity.END
        Direction.UP    -> Gravity.TOP
        Direction.FADE  -> Gravity.TOP // fallback
    }
}
