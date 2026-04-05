package com.launcher.projectvoid

import com.launcher.projectvoid.data.Prefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMatcherTest {

    @Test
    fun blankOrUnknownNameNeverMatches() {
        val destinationRoute = "com.launcher.projectvoid.HomeRoute"

        // Blank and null names are treated as invalid route identifiers and must never match.
        assertFalse(routeMatches(destinationRoute, ""))
        assertFalse(routeMatches(destinationRoute, "   "))
        assertFalse(routeMatches(destinationRoute, null))

        // Unknown route names should also never match the current destination route.
        assertFalse(routeMatches(destinationRoute, "UnknownRoute"))
    }

    @Test
    fun knownTypedRouteNameMatches() {
        val destinationRoute = "com.launcher.projectvoid.NotificationSummaryRoute"

        // This is the happy-path used by transition logic when swipe actions map to valid destinations.
        assertTrue(routeMatches(destinationRoute, "NotificationSummaryRoute"))
    }

    @Test
    fun reverseSwipeFromGestureOpenedScreenNavigatesHome() {
        val notesRoute = "com.launcher.projectvoid.NotesRoute"

        // If Notes is opened by the configured LEFT swipe action, a RIGHT swipe should return home.
        assertTrue(
            shouldNavigateHomeFromSwipe(
                currentRoute = notesRoute,
                swipeDirection = SwipeDirection.RIGHT,
                leftSwipeAction = Prefs.SwipeAction.NOTES,
                rightSwipeAction = Prefs.SwipeAction.WIDGETS
            )
        )
    }

    @Test
    fun homeRouteNeverTriggersReturnHomeGesture() {
        val homeRoute = "com.launcher.projectvoid.HomeRoute"

        // This protects Home back-gesture behavior from accidentally entering a horizontal transition path.
        assertFalse(
            shouldNavigateHomeFromSwipe(
                currentRoute = homeRoute,
                swipeDirection = SwipeDirection.RIGHT,
                leftSwipeAction = Prefs.SwipeAction.NOTES,
                rightSwipeAction = Prefs.SwipeAction.WIDGETS
            )
        )
    }
}
