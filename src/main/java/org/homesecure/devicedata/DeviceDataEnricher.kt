package org.homesecure.devicedata

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import redis.clients.jedis.Jedis
import java.io.IOException


class DeviceDataEnricher(private val enrichmentUrl: String) {
    private val httpClient = OkHttpClient.Builder().build()
    private val objectMapper = ObjectMapper()
    private val jedis = Jedis("localhost")

    fun enrich(deviceData: DeviceData): DeviceData {
        println("Enrichment started for ${deviceData.userAgent}")
        val cachedData = searchCache(deviceData)
        if(cachedData != null) return cachedData

        val url = enrichmentUrl + deviceData.userAgent
        val response: Response
        try {
             response = httpClient.newCall(Request.Builder().url(url).build()).execute();
        } catch (e: IOException) {
            throw RuntimeException("Failure executing request to url=$url", e);
        }

        val responseJson = objectMapper.readValue(response.body!!.string(), Map::class.java)
        println("response: $responseJson")
        if(responseJson.toString().contains("success=false")) {
            throw RuntimeException("Error requesting $enrichmentUrl: $responseJson")
        }

        deviceData.enrich(responseJson["device"] as Map<String, String>)
        updateCache(deviceData)
        println("Enrichment ended for ${deviceData.userAgent}")

        return deviceData
    }

    private fun updateCache(deviceData: DeviceData) {
        jedis.lpush(deviceData.userAgent, deviceData.toCsvLine())
    }

    private fun searchCache(deviceData: DeviceData): DeviceData? {
        val cachedCountInKey: Long? = jedis.llen(deviceData.userAgent) ?: null
        if(cachedCountInKey == null || cachedCountInKey == 0L) {
            return null
        }

        val deviceDataStr = jedis.lrange(deviceData.userAgent, 0, cachedCountInKey)
                .find { DeviceData.fromCsvLine(it) == deviceData }

        return if(deviceDataStr == null) null else DeviceData.fromCsvLine(deviceDataStr)
    }

}