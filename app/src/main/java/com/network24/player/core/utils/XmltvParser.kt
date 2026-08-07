package com.network24.player.core.utils

import android.util.Xml
import com.network24.player.core.database.entity.EpgEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object XmltvParser {
    // XMLTV time format: "20231024143000 +0000"
    private val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.ENGLISH)

    fun parse(inputStream: InputStream): List<EpgEntity> {
        val epgList = mutableListOf<EpgEntity>()
        val parser: XmlPullParser = Xml.newPullParser()

        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType

        // Temporary variables to hold data while we parse a <programme> block
        var currentChannelId = ""
        var currentStartTime = 0L
        var currentEndTime = 0L
        var currentTitle = ""
        var currentDesc = ""
        var currentText = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "programme") {
                        // Reset variables for the new programme
                        currentTitle = ""
                        currentDesc = ""
                        currentChannelId = parser.getAttributeValue(null, "channel") ?: ""
                        currentStartTime = parseTime(parser.getAttributeValue(null, "start"))
                        currentEndTime = parseTime(parser.getAttributeValue(null, "stop"))
                    }
                }
                XmlPullParser.TEXT -> {
                    currentText = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "title" -> currentTitle = currentText
                        "desc" -> currentDesc = currentText
                        "programme" -> {
                            // Now that we have all the data, create the immutable entity
                            val epgEntity = EpgEntity(
                                id = "$currentChannelId-$currentStartTime", // String ID
                                streamId = 0, // Required Legacy field
                                epgChannelId = currentChannelId,
                                title = currentTitle,
                                description = currentDesc,
                                startTimestamp = currentStartTime,
                                stopTimestamp = currentEndTime
                            )
                            epgList.add(epgEntity)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return epgList
    }

    private fun parseTime(timeStr: String?): Long {
        if (timeStr == null) return 0L
        return try {
            xmltvDateFormat.parse(timeStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
