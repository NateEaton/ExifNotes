/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.InsetDrawable
import android.location.Location
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.transition.TransitionSet
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.tommihirvonen.exifnotes.datastructures.Coordinates
import com.tommihirvonen.exifnotes.datastructures.Gear
import java.io.*
import kotlin.math.absoluteValue

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

fun <T> T.validate(vararg validations: (T) -> (Boolean)): Boolean =
    validations.map { it(this) }.all { it }

val Context.packageInfo: PackageInfo? get() {
    try {
        return if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName,
                PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

fun latLngOrNull(value: String): LatLng? =
    try {
        val (latString, lngString) = value.split(" ")
        val lat = latString.replace(",", ".").toDouble()
        val lng = lngString.replace(",", ".").toDouble()
        LatLng(lat, lng)
    } catch (e: Exception) {
        null
    }

val LatLng.decimalString: String get() = "$latitude $longitude"

val LatLng.coordinates: Coordinates
    get() {
    val latRef = if (latitude < 0) "S" else "N"
    val lngRef = if (longitude < 0) "W" else "E"
    val latComponents = Location.convert(latitude.absoluteValue, Location.FORMAT_SECONDS)
    val lngComponents = Location.convert(longitude.absoluteValue, Location.FORMAT_SECONDS)
    val (latDegrees, latMinutes, latSeconds) = latComponents.split(":")
    val (lngDegrees, lngMinutes, lngSeconds) = lngComponents.split(":")
    return Coordinates(
        latRef, latDegrees, latMinutes, latSeconds,
        lngRef, lngDegrees, lngMinutes, lngSeconds
    )
}

val LatLng.readableCoordinates: String get() {
    val stringBuilder = StringBuilder()
    val space = " "
    val components = coordinates
    stringBuilder.append(components.latitudeDegrees).append("°").append(space)
        .append(components.latitudeMinutes).append("'").append(space)
        .append(components.latitudeSeconds.replace(',', '.'))
        .append("\"").append(space)
    stringBuilder.append(components.latitudeRef).append(space)

    stringBuilder.append(components.longitudeDegrees).append("°").append(space)
        .append(components.longitudeMinutes).append("'").append(space)
        .append(components.longitudeSeconds.replace(',', '.'))
        .append("\"").append(space)
    stringBuilder.append(components.longitudeRef)
    return stringBuilder.toString()
}

val LatLng.exifToolLocation: String get() {
    val stringBuilder = StringBuilder()
    val quote = "\""
    val space = " "
    val gpsLatTag = "-GPSLatitude="
    val gpsLatRefTag = "-GPSLatitudeRef="
    val gpsLngTag = "-GPSLongitude="
    val gpsLngRefTag = "-GPSLongitudeRef="
    val components = coordinates
    stringBuilder.append(gpsLatTag).append(quote).append(components.latitudeDegrees)
        .append(space).append(components.latitudeMinutes).append(space)
        .append(components.latitudeSeconds).append(quote).append(space)
    stringBuilder.append(gpsLatRefTag).append(quote).append(components.latitudeRef).append(quote).append(space)

    stringBuilder.append(gpsLngTag).append(quote).append(components.longitudeDegrees)
        .append(space).append(components.longitudeMinutes).append(space)
        .append(components.longitudeSeconds).append(quote).append(space)
    stringBuilder.append(gpsLngRefTag).append(quote).append(components.longitudeRef).append(quote).append(space)
    return stringBuilder.toString()
}

/**
 * Removes potential illegal characters from a string to make it a valid file name.
 */
fun String.illegalCharsRemoved(): String = replace("[|\\\\?*<\":>/]".toRegex(), "_")

/**
 * Remove all files in a directory. Subdirectories are skipped.
 */
fun File.purgeDirectory() = this.listFiles()?.filterNot(File::isDirectory)?.forEach { it.delete() }

fun File.makeDirsIfNotExists() { if (!isDirectory) mkdirs() }

fun List<Gear>.toStringList(): String = if (this.isEmpty()) {
    ""
} else {
    this.joinToString(separator = "\n-", prefix = "\n-", transform = Gear::name)
}

fun TransitionSet.setCommonInterpolator(interpolator: Interpolator): TransitionSet = apply {
    (0 until transitionCount).map(::getTransitionAt).forEach { transition ->
        transition?.interpolator = interpolator
    }
}

fun View.snackbar(@StringRes resId: Int, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, resId, duration).show()

fun View.snackbar(text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, text, duration).show()

fun View.snackbar(@StringRes resId: Int, anchorView: View, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, resId, duration).setAnchorView(anchorView).show()

fun View.snackbar(text: CharSequence, anchorView: View, duration: Int = Snackbar.LENGTH_LONG) =
    Snackbar.make(this, text, duration).setAnchorView(anchorView).show()

fun <T> List<T>.isEmptyOrContains(value: T): Boolean = contains(value) || isEmpty()

fun <T> List<T>.applyPredicates(vararg predicates: ((T) -> (Boolean))): List<T> =
    filter { item -> predicates.all { p -> p(item) } }

fun <T, U : Comparable<U>> List<T>.mapDistinct(transform: (T) -> U): List<U> =
    map(transform).distinct().sorted()

@SuppressLint("RestrictedApi")
fun PopupMenu.setIconsVisible(context: Context) {
    val iconMargin = 0f
    if (menu is MenuBuilder) {
        val menuBuilder = menu as MenuBuilder
        menuBuilder.setOptionalIconsVisible(true)
        for (item in menuBuilder.visibleItems) {
            val iconMarginPx =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, iconMargin, context.resources.displayMetrics)
                    .toInt()
            if (item.icon != null) {
                if (SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx,0)
                } else {
                    item.icon =
                        object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                            override fun getIntrinsicWidth(): Int {
                                return intrinsicHeight + iconMarginPx + iconMarginPx
                            }
                        }
                }
            }
        }
    }
}