package net.azisaba.spicyAzisaBan.punishment

import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.TableData

data class Proof(
    val id: Long,
    val punishment: Punishment,
    val text: String,
) {
    companion object {
        fun fromTableData(td: TableData): Promise<Proof> = Promise.create { context ->
            val id = td.getLong("id")!!
            val punishId = td.getLong("punish_id")!!
            val text = td.getString("text")!!
            val p = Punishment.fetchPunishmentById(punishId).complete()
                ?: return@create context.reject(IllegalArgumentException("Missing punishment $punishId"))
            context.resolve(Proof(id, p, text))
        }

        fun fromTableData(punishment: Punishment, td: TableData): Proof {
            val id = td.getLong("id")!!
            val punishId = td.getLong("punish_id")!!
            val text = td.getString("text")!!
            if (punishment.id != punishId) throw IllegalArgumentException("Wrong punishment ${punishment.id} != $punishId")
            return Proof(id, punishment, text)
        }
    }
}
