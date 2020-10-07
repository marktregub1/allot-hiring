package org.homesecure

import org.homesecure.devicedata.DeviceData
import org.homesecure.devicedata.DeviceDataManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DataDeviceManagerTest {
    private val devicesCount=20
    @Test
    fun testDataLoad() {
        val deviceDataManager = DeviceDataManager(javaClass.getResourceAsStream("/devices.csv"))
        Assertions.assertEquals(devicesCount, deviceDataManager.devices.size)
    }

    @Test
    fun testReadWriteCsv() {
        val csvLine = "\"5acb5b26a792419c7eccfa1a\",\"IPHONE\",\"71:36:9E:7D:41:09\",\"server-bag [iPhone OS,11.2.6,15D100,iPhone9,3]\""
        val deviceData = DeviceData.fromCsvLine(csvLine)

        deviceData.enrich( DeviceDataManager.enrichmentFieldsList.map { it to "null" }.toMap())

        val csvLineAfterTransform = DeviceData.fromCsvLine(csvLine).toCsvLine()
        Assertions.assertEquals(deviceData.toCsvLine(), csvLineAfterTransform)
    }
}