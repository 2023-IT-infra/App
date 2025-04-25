class KalmanFilter(
    private var processNoise: Double = 0.05,         // 프로세스 노이즈
    private var measurementNoise: Double = 80.0,      // 측정 노이즈
    private val initSamples: Int = 30,                 // 초기화에 사용할 측정 샘플 수
    private val resetThreshold: Double = 10.0         // 이상치 감지를 위한 임계치 (dB)
) {
    private var initialized = false
    private var predictedRSSI: Double = 0.0
    private var errorCovariance: Double = 0.0
    private var sampleCount = 0
    private var initSum = 0.0

    /**
     * 칼만 필터링 함수.
     * 초기 측정값들을 이용해 초기 상태를 설정한 후 칼만 필터 알고리즘을 적용합니다.
     */
    fun filtering(rssi: Double): Double {
        // 초기화 전: 일정 개수의 초기 샘플을 누적하여 평균값을 초기 상태로 설정
        if (!initialized) {
            sampleCount++
            initSum += rssi
            if (sampleCount >= initSamples) {
                predictedRSSI = initSum / sampleCount
                errorCovariance = 1.0
                initialized = true
            }
            return predictedRSSI
        }

        // 측정값과 예측값 차이가 너무 크면 (이상치) 필터를 재설정합니다.
        if (Math.abs(rssi - predictedRSSI) > resetThreshold) {
            reset()
            // 재설정 후 현재 측정값을 이용해 초기화 진행
            sampleCount++
            initSum += rssi
            if (sampleCount >= initSamples) {
                predictedRSSI = initSum / sampleCount
                errorCovariance = 1.0
                initialized = true
            }
            return predictedRSSI
        }

        // 칼만 필터 예측 단계
        val priorRSSI = predictedRSSI
        val priorErrorCovariance = errorCovariance + processNoise

        // 칼만 이득 계산
        val kalmanGain = priorErrorCovariance / (priorErrorCovariance + measurementNoise)

        // 업데이트 단계: 측정값을 반영하여 상태와 오차 공분산 갱신
        predictedRSSI = priorRSSI + kalmanGain * (rssi - priorRSSI)
        errorCovariance = (1 - kalmanGain) * priorErrorCovariance

        // (선택사항) 적응적 측정 노이즈 조정 로직:
        // measurementNoise = measurementNoise * 0.99 + 0.01 * Math.pow(rssi - predictedRSSI, 2.0)

        return predictedRSSI
    }

    /**
     * 칼만 필터 초기화 함수.
     */
    fun reset() {
        initialized = false
        errorCovariance = 0.0
        sampleCount = 0
        initSum = 0.0
    }
}