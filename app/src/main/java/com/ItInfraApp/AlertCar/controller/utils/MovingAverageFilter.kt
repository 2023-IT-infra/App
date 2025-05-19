package com.ItInfraApp.AlertCar.controller.utils

import java.util.ArrayDeque
import kotlin.math.abs

class MovingAverageFilter(
    private val windowSize: Int = 23,
    private val outlierThreshold: Double = 10.0,    // 직전값 대비 ±10dB 이상은 우선 이상치
    private val outlierMaxCount: Int = 5            // 연속 이상치 허용 횟수
) {
    private val window = ArrayDeque<Double>()
    private var lastValidRssi: Double? = null
    private var consecutiveOutlierCount = 0

    /**
     * 필터링 메소드
     * @param rssi 측정된 RSSI (dBm)
     * @return 평균화된 RSSI
     */
    fun filter(rssi: Double): Double {
        // 1) 초기값 세팅
        if (lastValidRssi == null) {
            lastValidRssi = rssi
            window.add(rssi)
            return rssi
        }

        // 2) 이상치 판단
        val diff = abs(rssi - lastValidRssi!!)
        val isOutlier = diff > outlierThreshold

        // 3) 연속 이상치 카운트
        if (isOutlier) {
            consecutiveOutlierCount++
        } else {
            consecutiveOutlierCount = 0
        }

        // 4) 정상값이거나, 연속 이상치가 maxCount를 넘으면 수용
        if (!isOutlier || consecutiveOutlierCount >= outlierMaxCount) {
            window.add(rssi)
            lastValidRssi = rssi
            consecutiveOutlierCount = 0  // 리셋
        }

        // 5) 윈도우 크기 유지
        if (window.size > windowSize) {
            window.removeFirst()
        }

        // 6) 평균 반환
        return window.average()
    }
}