package net.azisaba.spicyAzisaBan.punishment

import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.toMinecraft
import util.promise.rewrite.Promise
import xyz.acrylicstyle.sql.TableData

data class Proof(
    val id: Long,
    val punishment: Punishment,
    val text: String,
    val public: Boolean,
) {
    val variables = mapOf(
        "id" to id.toString(),
        "pid" to punishment.id.toString(),
        "text" to text,
        "public" to public.toMinecraft(),
    )

    companion object {
        fun fromTableData(td: TableData): Promise<Proof> = async { context ->
            val id = td.getLong("id")!!
            val punishId = td.getLong("punish_id")!!
            val text = td.getString("text")!!
            val p = Punishment.fetchPunishmentById(punishId).complete()
                ?: return@async context.reject(IllegalArgumentException("Missing punishment $punishId"))
            val public = td.getBoolean("public")
            context.resolve(Proof(id, p, text, public))
        }

        fun fromTableData(punishment: Punishment, td: TableData): Proof {
            val id = td.getLong("id")!!
            val punishId = td.getLong("punish_id")!!
            if (punishment.id != punishId) throw IllegalArgumentException("Wrong punishment ${punishment.id} != $punishId")
            val text = td.getString("text")!!
            val public = td.getBoolean("public")
            return Proof(id, punishment, text, public)
        }
    }
}
