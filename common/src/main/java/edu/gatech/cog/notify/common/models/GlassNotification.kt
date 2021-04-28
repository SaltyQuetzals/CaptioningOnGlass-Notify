package edu.gatech.cog.notify.common.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class GlassNotification(
    val text: String,
    val isVibrate: Boolean,
    val isClear: Boolean
) : Parcelable, Serializable