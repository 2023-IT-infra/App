package com.ItInfraApp.AlertCar.controller.utils

class KalmanFilter(
    private val processNoise: Double = 1.0,
    private val measurementNoise: Double = 10.0
) {
    private var initialized = false
    private var predictedRSSI = 0.0
    private var errorCovariance = 1.0

    fun filter(rssi: Double): Double {
        if (!initialized) {
            predictedRSSI = rssi
            errorCovariance = 1.0
            initialized = true
        } else {
            val priorCov = errorCovariance + processNoise
            val kalmanGain = priorCov / (priorCov + measurementNoise)
            predictedRSSI += kalmanGain * (rssi - predictedRSSI)
            errorCovariance = (1 - kalmanGain) * priorCov
        }
        return predictedRSSI
    }
}