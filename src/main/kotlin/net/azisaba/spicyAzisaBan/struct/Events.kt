package net.azisaba.spicyAzisaBan.struct

import org.json.JSONObject
import java.sql.ResultSet

data class Events(
    val id: Long,
    val event: EventType,
    val data: JSONObject,
    val seen: String,
) {
    companion object {
        fun fromResultSet(rs: ResultSet): Events {
            val id = rs.getLong("id")
            val event = EventType.valueOf(rs.getString("event_id").uppercase())
            val data = JSONObject(rs.getString("data"))
            val seen = rs.getString("seen")!!
            return Events(id, event, data, seen)
        }
    }
}
