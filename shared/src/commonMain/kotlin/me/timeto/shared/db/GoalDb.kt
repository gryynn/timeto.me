package me.timeto.shared.db

import dbsq.GoalSq
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.*
import me.timeto.shared.UnixTime

data class GoalDb(
    val id: Int,
    val activity_id: Int,
    val seconds: Int,
    val period_json: String,
    val note: String,
    val finish_text: String,
) {

    companion object {

        suspend fun selectAll(): List<GoalDb> = dbIo {
            db.goalQueries.selectAll().executeAsList().map { it.toDb() }
        }

        fun selectAllFlow(): Flow<List<GoalDb>> =
            db.goalQueries.selectAll().asListFlow { it.toDb() }

        suspend fun insertWithValidation(
            activityDb: ActivityDb,
            seconds: Int,
            period: Period,
            note: String,
            finishText: String,
        ): Unit = dbIo {
            db.goalQueries.insert(
                activity_id = activityDb.id,
                seconds = seconds,
                period_json = period.toJson().toString(),
                note = note.trim(),
                finish_text = finishText.trim(),
            )
        }
    }

    fun buildPeriod(): Period =
        Period.fromJson(Json.parseToJsonElement(period_json).jsonObject)

    ///

    sealed interface Period {

        fun isToday(): Boolean

        fun note(): String

        fun toJson(): JsonObject

        ///

        enum class Type(val id: Int) {
            daysOfWeek(1),
        }

        companion object {

            fun fromJson(json: JsonObject): Period {
                val typeRaw: Int = json["type"]!!.jsonPrimitive.int
                return when (typeRaw) {
                    Type.daysOfWeek.id -> DaysOfWeek.fromJson(json)
                    else -> throw Exception("GoalDb.Period.fromJson() type: $typeRaw")
                }
            }
        }

        ///

        class DaysOfWeek(
            val weekDays: List<Int>,
        ) : Period {

            companion object {

                fun fromJson(json: JsonObject) = DaysOfWeek(
                    weekDays = json["days"]!!.jsonArray.map { it.jsonPrimitive.int },
                )
            }

            ///

            override fun isToday(): Boolean =
                UnixTime().dayOfWeek() in weekDays

            override fun note(): String {
                if (weekDays.size == 7)
                    return "Every Day"
                // todo if size is zero?
                return weekDays.map { UnixTime.dayOfWeekNames2 }.joinToString(", ")
            }

            override fun toJson() = JsonObject(
                mapOf(
                    "type" to JsonPrimitive(Type.daysOfWeek.id),
                    "days" to JsonArray(weekDays.map { JsonPrimitive(it) }),
                )
            )
        }
    }
}

private fun GoalSq.toDb() = GoalDb(
    id = id,
    activity_id = activity_id,
    seconds = seconds,
    period_json = period_json,
    note = note,
    finish_text = finish_text,
)
