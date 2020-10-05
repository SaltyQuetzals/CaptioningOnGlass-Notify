package edu.gatech.cog.notify.common.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GlassNotification(
    val text: String,
    val isVibrate: Boolean,
) : Parcelable {

    companion object {
        fun convert(string: String): GlassNotification {
            val contents = string.split(";")
            return GlassNotification(contents[0], contents[1].toBoolean())
        }

        fun convert(glassNotification: GlassNotification): String {
            return "${glassNotification.text};${glassNotification.isVibrate}"
        }
    }
}