package edu.gatech.cog.notify.common.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.regex.Pattern

@Parcelize
data class GlassNotification(
    val text: String,
    val isVibrate: Boolean,
) : Parcelable {

    companion object {
        fun convert(string: String): GlassNotification {
            val contents = string.split(Pattern.quote("|"))
            return GlassNotification(contents[0], contents[1].toBoolean())
        }

        fun convert(glassNotification: GlassNotification): String {
            return "${glassNotification.text}|${glassNotification.isVibrate}"
        }
    }
}