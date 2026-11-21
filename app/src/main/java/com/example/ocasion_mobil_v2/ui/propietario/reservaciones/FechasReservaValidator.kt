package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object FechasReserva {

    fun desdeCalendario(milisegundos: Long): LocalDate {
        return Instant.ofEpochMilli(milisegundos)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    }

    fun paraCalendario(fecha: LocalDate): Long {
        return fecha.atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
}

class FechasReservaValidator(
    private val fechasBloqueadas: LongArray
) : CalendarConstraints.DateValidator {

    private val bloqueos = fechasBloqueadas.toSet()

    override fun isValid(date: Long): Boolean {
        val fecha = FechasReserva.desdeCalendario(date)

        return !fecha.isBefore(LocalDate.now()) &&
                fecha.toEpochDay() !in bloqueos
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLongArray(fechasBloqueadas)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<FechasReservaValidator> {

            override fun createFromParcel(
                source: Parcel
            ): FechasReservaValidator {
                return FechasReservaValidator(
                    source.createLongArray() ?: longArrayOf()
                )
            }

            override fun newArray(
                size: Int
            ): Array<FechasReservaValidator?> {
                return arrayOfNulls(size)
            }
        }
    }
}