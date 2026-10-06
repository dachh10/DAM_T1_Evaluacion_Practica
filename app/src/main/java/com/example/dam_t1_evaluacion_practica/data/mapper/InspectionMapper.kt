package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.ModuleCompletion
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import org.json.JSONArray
import org.json.JSONObject

fun InspectionEntity.toDomainModel(): InspectionSessionRecord {
    return InspectionSessionRecord(
        folio = folio,
        userEmail = userEmail,
        companyName = companyName,
        zoneName = zoneName,
        auditType = auditType,
        inspectorName = inspectorName,
        inspectorCedula = inspectorCedula,
        timestamp = timestamp,
        overallScore = overallScore,
        criticalStop = criticalStop,
        reinspectionDue = reinspectionDue,
        scheduledReinspectionAt = scheduledReinspectionAt,
        modules = parseModulesJson(modulesJson),
        findings = parseFindingsJson(findingsJson),
        normCodes = parseNormCodesJson(normCodesJson),
        latitude = latitude,
        longitude = longitude,
        status = try {
            IncidenceStatus.valueOf(status)
        } catch (e: Exception) {
            IncidenceStatus.OPEN
        }
    )
}

fun InspectionSessionRecord.toEntity(): InspectionEntity {
    return InspectionEntity(
        folio = folio,
        userEmail = userEmail,
        companyName = companyName,
        zoneName = zoneName,
        auditType = auditType,
        inspectorName = inspectorName,
        inspectorCedula = inspectorCedula,
        timestamp = timestamp,
        overallScore = overallScore,
        criticalStop = criticalStop,
        reinspectionDue = reinspectionDue,
        scheduledReinspectionAt = scheduledReinspectionAt,
        status = status.name,
        latitude = latitude,
        longitude = longitude,
        modulesJson = modulesToJson(modules),
        findingsJson = findingsToJson(findings),
        normCodesJson = normCodesToJson(normCodes)
    )
}

private fun normCodesToJson(codes: List<String>): String {
    val array = JSONArray()
    codes.forEach { array.put(it) }
    return array.toString()
}

private fun parseNormCodesJson(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<String>()
    return try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun findingsToJson(findings: List<SessionFinding>): String {
    val array = JSONArray()
    findings.forEach { f ->
        val obj = JSONObject().apply {
            put("itemId", f.itemId)
            put("itemDescription", f.itemDescription)
            put("normCode", f.normCode)
            put("criticalityLevel", f.criticalityLevel.name)
            put("isNA", f.isNA)
            put("notes", f.notes ?: "")
            put("photoUri", f.photoUri ?: "")
            put("timestamp", f.timestamp)
            if (f.latitude != null) put("latitude", f.latitude)
            if (f.longitude != null) put("longitude", f.longitude)
        }
        array.put(obj)
    }
    return array.toString()
}

private fun parseFindingsJson(json: String): List<SessionFinding> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<SessionFinding>()
    return try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SessionFinding(
                    itemId = obj.optString("itemId"),
                    itemDescription = obj.optString("itemDescription"),
                    normCode = obj.optString("normCode"),
                    criticalityLevel = try {
                        CriticalityLevel.valueOf(obj.optString("criticalityLevel", "LOW"))
                    } catch (e: Exception) {
                        CriticalityLevel.LOW
                    },
                    isNA = obj.optBoolean("isNA", false),
                    notes = obj.optString("notes").ifBlank { null },
                    photoUri = obj.optString("photoUri").ifBlank { null },
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                    longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun modulesToJson(modules: List<ModuleCompletion>): String {
    val array = JSONArray()
    modules.forEach { m ->
        val obj = JSONObject().apply {
            put("moduleTitle", m.moduleTitle)
            put("normCodes", JSONArray(m.normCodes))
            put("score", m.score)
            put("criticalStop", m.criticalStop)
            put("reinspectionDue", m.reinspectionDue)
            put("completed", m.completed)
            
            val ansObj = JSONObject()
            m.answers.forEach { (k, v) -> ansObj.put(k, v) }
            put("answers", ansObj)

            val naArr = JSONArray()
            m.naAnswers.forEach { naArr.put(it) }
            put("naAnswers", naArr)

            val itemsArr = JSONArray()
            m.items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("id", item.id)
                    put("description", item.description)
                    put("normCode", item.normativ.code)
                    put("normTitle", item.normativ.title)
                    put("criticalityLevel", item.criticalityLevel.name)
                    put("requiresPhotoEvidence", item.requiresPhotoEvidence)
                }
                itemsArr.put(itemObj)
            }
            put("items", itemsArr)
        }
        array.put(obj)
    }
    return array.toString()
}

private fun parseModulesJson(json: String): List<ModuleCompletion> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<ModuleCompletion>()
    return try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            val normCodesList = mutableListOf<String>()
            val normCodesArr = obj.optJSONArray("normCodes")
            if (normCodesArr != null) {
                for (j in 0 until normCodesArr.length()) {
                    normCodesList.add(normCodesArr.getString(j))
                }
            }

            val answersMap = mutableMapOf<String, Boolean>()
            val ansObj = obj.optJSONObject("answers")
            if (ansObj != null) {
                val keys = ansObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    answersMap[k] = ansObj.getBoolean(k)
                }
            }

            val naSet = mutableSetOf<String>()
            val naArr = obj.optJSONArray("naAnswers")
            if (naArr != null) {
                for (j in 0 until naArr.length()) {
                    naSet.add(naArr.getString(j))
                }
            }

            val itemsList = mutableListOf<ChecklistItem>()
            val itemsArr = obj.optJSONArray("items")
            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(j)
                    itemsList.add(
                        ChecklistItem(
                            id = itemObj.optString("id"),
                            description = itemObj.optString("description"),
                            normativ = Normativ(
                                code = itemObj.optString("normCode"),
                                title = itemObj.optString("normTitle")
                            ),
                            criticalityLevel = try {
                                CriticalityLevel.valueOf(itemObj.optString("criticalityLevel", "LOW"))
                            } catch (e: Exception) {
                                CriticalityLevel.LOW
                            },
                            requiresPhotoEvidence = itemObj.optBoolean("requiresPhotoEvidence", false)
                        )
                    )
                }
            }

            list.add(
                ModuleCompletion(
                    moduleTitle = obj.optString("moduleTitle"),
                    normCodes = normCodesList,
                    items = itemsList,
                    answers = answersMap,
                    naAnswers = naSet,
                    score = obj.optInt("score"),
                    criticalStop = obj.optBoolean("criticalStop"),
                    reinspectionDue = obj.optBoolean("reinspectionDue"),
                    completed = obj.optBoolean("completed")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}
