package com.example.appblocker.utils

import com.example.appblocker.model.TimeRange
import org.json.JSONArray
import org.json.JSONObject

object TimeRangeStorage {

    fun serialize(ranges: List<TimeRange>): String {
        val jsonArray = JSONArray()
        for (range in ranges) {
            val obj = JSONObject()
            obj.put("startHour", range.startHour)
            obj.put("startMinute", range.startMinute)
            obj.put("endHour", range.endHour)
            obj.put("endMinute", range.endMinute)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserialize(json: String): List<TimeRange> {
        val result = mutableListOf<TimeRange>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startHour = obj.getInt("startHour")
                val startMinute = obj.getInt("startMinute")
                val endHour = obj.getInt("endHour")
                val endMinute = obj.getInt("endMinute")
                result.add(TimeRange(startHour, startMinute, endHour, endMinute))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
