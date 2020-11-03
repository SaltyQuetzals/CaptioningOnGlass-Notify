package edu.gatech.cog.notify.glass

/**
 * Wrapper for GlassGestureDetector.Gesture
 * EventBus doesn't take in enums
 */
data class GlassGesture(val gesture: GlassGestureDetector.Gesture)