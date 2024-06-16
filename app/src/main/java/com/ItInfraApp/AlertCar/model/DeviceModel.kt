package com.ItInfraApp.AlertCar.model

/**
 * Represents a device with its properties.
 *
 * @property name The name of the device. Default value is "Unknown".
 * @property address The address of the device. Default value is "Unknown".
 * @property rssi The RSSI (Received Signal Strength Indication) of the device. Default value is 0.
 */
data class DeviceModel(
    val name: String = "Unknown",
    val address: String = "Unknown",
    val rssi: Int = 0
) {
    /**
     * Checks if this [DeviceModel] is equal to another object.
     *
     * @param other The other object to compare.
     * @return `false` if the objects are not equal, `true` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceModel

        if (name != other.name) return false
        if (address != other.address) return false
        if (rssi != other.rssi) return false

        return true
    }

    /**
     * Generates a hash code for this DeviceModel.
     *
     * @return The hash code value.
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + rssi
        return result
    }
}

