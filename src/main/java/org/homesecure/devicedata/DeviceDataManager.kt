package org.homesecure.devicedata

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.homesecure.devicedata.DeviceDataManager.Companion.enrichmentFieldsList
import org.homesecure.devicedata.DeviceDataManager.Companion.rawFieldsList
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

//dfc79b8cfa10fc76dfb17caee35f13d0
class DeviceDataManager {
/*
 "device": {
        "is_mobile_device": false,
        "type": "smarttv",
        "brand": "Apple",
        "brand_code": "apple",
        "brand_url": "https://www.apple.com/",
        "name": "TV"
    }
 */
    companion object {
        val rawFieldsList = listOf("id", "name", "mac", "userAgent")
        val enrichmentFieldsList = listOf("is_mobile_device", "type", "brand", "brand_code", "brand_url")
    }
    val devices = mutableListOf<DeviceData>()

    constructor(csvFileStream: InputStream) {
        devices.clear()
        reloadData(csvFileStream)
    }

    private fun reloadData(inputStream: InputStream) {
        val parser = CSVParser.parse(
                inputStream, Charset.forName("UTF-8"),
                CSVFormat.DEFAULT.withFirstRecordAsHeader())

        for(record in parser) devices.add(DeviceData(record.toMap()))

    }

    fun enrichData(enricher: DeviceDataEnricher, outputStream: OutputStream) {
        outputStream.write(
                rawFieldsList.plus(enrichmentFieldsList).plus("\n").joinToString(",").toByteArray())

//        runBlocking {
//            devices.forEach {
//                GlobalScope.async {
//                    outputStream.write(enricher.enrich(it).toCsvLine().toByteArray())
//                }
//            }
//        }

        devices.forEach {
            outputStream.write(enricher.enrich(it).toCsvLine().toByteArray())
        }
    }
}

data class DeviceData(val rawFields: Map<String, String>, val userAgent: String? = rawFields["userAgent"]) {
    var enrichmentData: MutableMap<String, String> = mutableMapOf()

    fun enrich(jsonMap: Map<*, *>) {
        enrichmentFieldsList.forEach {
            this.enrichmentData[it] = jsonMap[it].toString()
        }
    }

    fun toCsvLine() = "${toCsvLine(rawFieldsList, rawFields)},${toCsvLine(enrichmentFieldsList, enrichmentData)}\n"


    private fun toCsvLine(order: List<String>, data: Map<String, String>) =
            order.map { "\"${data[it]}\"" }.joinToString(",")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceData) return false

        if (rawFields != other.rawFields) return false

        return true
    }

    override fun hashCode(): Int {
        return rawFields.hashCode()
    }


    companion object {
        fun fromCsvLine(csvLine: String): DeviceData {
            var rawFields: MutableMap<String, String> = mutableMapOf()
            var enrichmentData: MutableMap<String, String> = mutableMapOf()
            var counter=0
            csvLine.substring(1, csvLine.length-2).split("\",\"").forEach {
                if(counter < rawFieldsList.size) rawFields[rawFieldsList[counter]] = it
                else enrichmentData[enrichmentFieldsList[counter - rawFieldsList.size]] = it
                counter++
            }
            var deviceData = DeviceData(rawFields)
            deviceData.enrich(enrichmentData)
            return deviceData
        }
    }
}
