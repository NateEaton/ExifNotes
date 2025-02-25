/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes

import com.google.android.gms.maps.model.LatLng
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.FilmProcess
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.FilmType
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.datastructures.Format
import com.tommihirvonen.exifnotes.datastructures.Frame
import com.tommihirvonen.exifnotes.datastructures.Increment
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.LightSource
import com.tommihirvonen.exifnotes.datastructures.PartialIncrement
import com.tommihirvonen.exifnotes.datastructures.Roll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.LocalDateTime

class RollTest {
    @Test
    fun roll_serialize() {
        val camera = Camera(
            id = 1,
            make = "Canon",
            model = "A-1",
            serialNumber = "ABC123",
            minShutter = "1/1000",
            maxShutter = "30",
            shutterIncrements = Increment.HALF,
            exposureCompIncrements = PartialIncrement.THIRD,
            format = Format.MM35
        )
        val filmStock = FilmStock(
            id = 2,
            make = "ILFORD",
            model = "HP5+",
            iso = 400,
            type = FilmType.BW_NEGATIVE,
            process = FilmProcess.BW_NEGATIVE,
            isPreadded = true
        )
        val lens = Lens(
            id = 4,
            make = "Canon",
            model = "FD 28mm f/2.8",
            minAperture = "2.8",
            maxAperture = "2.8",
            minFocalLength = 28,
            maxFocalLength = 28,
            serialNumber = "ABC123",
            apertureIncrements = Increment.HALF
        )
        val filters = listOf(
            Filter(id = 10, make = "Haida", model = "C-POL PRO II"),
            Filter(id = 11, make = "Hoya", model = "ND x64")
        )
        val roll = Roll(
            id = 3,
            name = "Test roll name",
            date = LocalDateTime.now(),
            unloaded = LocalDateTime.now(),
            developed = LocalDateTime.now(),
            note = "Test roll description",
            camera = camera,
            iso = 400,
            pushPull = "+1/3",
            format = Format.MM35,
            archived = false,
            filmStock = filmStock,
            frames = listOf(
                Frame(
                    id = 5,
                    count = 10,
                    date = LocalDateTime.now(),
                    shutter = "1/125",
                    aperture = "2.8",
                    note = "Test frame description",
                    location = LatLng(35.0, 35.0),
                    formattedAddress = "",
                    focalLength = 28,
                    exposureComp = "+1/3",
                    noOfExposures = 1,
                    flashUsed = false,
                    lightSource = LightSource.SUNNY,
                    lens = lens,
                    filters = filters
                ),
                Frame(
                    id = 6,
                    count = 11,
                    date = LocalDateTime.now(),
                    shutter = "1/125",
                    aperture = "2.8",
                    note = "Test frame description",
                    location = LatLng(35.0, 35.0),
                    formattedAddress = "",
                    focalLength = 28,
                    exposureComp = "+1/3",
                    noOfExposures = 1,
                    flashUsed = false,
                    lightSource = LightSource.SUNNY,
                    lens = lens,
                    filters = filters
                )
            )
        )
        val format = Json { prettyPrint = true }
        val rollJson = format.encodeToString(roll)
        println(rollJson)
    }
}