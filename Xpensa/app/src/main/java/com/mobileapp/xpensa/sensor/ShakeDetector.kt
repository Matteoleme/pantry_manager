package com.mobileapp.xpensa.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Listener per il sensore Accelerometro [Sensor.TYPE_ACCELEROMETER].
 * Rileva il movimento di scuotimento ("shake") del dispositivo ed esegue [onShake].
 */
class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {

    private var lastShakeTimestamp: Long = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if ((event == null) || (event.sensor.type != Sensor.TYPE_ACCELEROMETER)) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Forza g = ~1 in condizioni stazionarie
        val gForce = sqrt((gX * gX) + (gY * gY) + (gZ * gZ))

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            // Ignora shake troppo ravvicinati (cooldown di 1000ms)
            if ((lastShakeTimestamp + SHAKE_SLOP_TIME_MS) > now) {
                return
            }

            lastShakeTimestamp = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non necessario per l'accelerometro
    }

    companion object {
        /** Soglia di sensibilità gForce per attivare lo shake */
        private const val SHAKE_THRESHOLD_GRAVITY = 2.5f

        /** Cooldown in millisecondi per evitare registrazioni doppie */
        private const val SHAKE_SLOP_TIME_MS = 1000
    }
}
