class KalmanFilter(
    private var processNoise: Double = 0.08,
    private var measurementNoise: Double = 10.0
) {
    private var initialized = false
    private var predictedRSSI: Double = 0.0
    private var errorCovariance: Double = 0.0

    fun filtering(rssi: Double): Double {
        val priorRSSI: Double
        val priorErrorCovariance: Double

        if (!initialized) {
            initialized = true
            priorRSSI = rssi
            priorErrorCovariance = 1.0
        } else {
            priorRSSI = predictedRSSI
            priorErrorCovariance = errorCovariance + processNoise
        }

        val kalmanGain = priorErrorCovariance / (priorErrorCovariance + measurementNoise)
        predictedRSSI = priorRSSI + (kalmanGain * (rssi - priorRSSI))
        errorCovariance = (1 - kalmanGain) * priorErrorCovariance

        return predictedRSSI
    }
}