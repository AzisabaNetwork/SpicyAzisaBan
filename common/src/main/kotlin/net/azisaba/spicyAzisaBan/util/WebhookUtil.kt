package net.azisaba.spicyAzisaBan.util

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.punishment.Expiration
import net.azisaba.spicyAzisaBan.punishment.Proof
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import util.http.DiscordWebhook
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import java.awt.Color

object WebhookUtil {
    fun Punishment.sendWebhook(): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(server, type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return operator.getProfile().then { profile ->
                webhook.username = profile.name
                webhook.content = "処罰が追加されました。"
                val embed = DiscordWebhook.EmbedObject()
                embed.color = Color.RED
                embed.addField("種類", "${type.getName()} (${type.name})", false)
                embed.addField("処罰執行者", "${profile.name} (${profile.uniqueId})", false)
                embed.addField("対象サーバー", server, false)
                embed.addField("被処罰者", name, false)
                embed.addField("理由", reason, false)
                embed.addField("ID", id.toString(), false)
                embed.addField("処罰日時", SABMessages.formatDate(start), false)
                if (type.name.contains("TEMP")) {
                    embed.addField("期間", Util.unProcessTime(end.serializeAsLong() - start), false)
                    embed.addField("期限", if (end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(end.serializeAsLong()), false)
                }
                webhook.addEmbed(embed)
                webhook.execute()
            }.then {}.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    fun Punishment.sendReasonChangedWebhook(actor: Actor, newReason: String): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(server, type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return async<Unit> { context ->
                webhook.username = actor.name
                webhook.content = "処罰理由が変更されました。"
                val embed = DiscordWebhook.EmbedObject()
                embed.color = Color.ORANGE
                embed.addField("種類", "${type.getName()} (${type.name})", false)
                embed.addField("コマンド実行者", "${actor.name} (${actor.uniqueId})", false)
                embed.addField("対象サーバー", server, false)
                embed.addField("被処罰者", name, false)
                embed.addField("新しい理由", newReason, false)
                embed.addField("元の理由", reason, false)
                embed.addField("ID", id.toString(), false)
                embed.addField("処罰日時", SABMessages.formatDate(start), false)
                if (type.name.contains("TEMP")) {
                    embed.addField("期間", Util.unProcessTime(end.serializeAsLong() - start), false)
                    embed.addField("期限", if (end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(end.serializeAsLong()), false)
                }
                webhook.addEmbed(embed)
                try {
                    webhook.execute()
                    context.resolve()
                } catch (e: Throwable) {
                    context.reject(e)
                }
            }.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    fun UnPunish.sendWebhook(): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(punishment.server, punishment.type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return punishment.operator.getProfile().then { profile ->
                val unPunishOpProfile = operator.getProfile().complete()
                webhook.username = unPunishOpProfile.name
                webhook.content = "処罰が解除されました。"
                val embed = DiscordWebhook.EmbedObject()
                embed.color = Color.GREEN
                embed.addField("種類", "${punishment.type.getName()} (${punishment.type.name})", false)
                embed.addField("対象サーバー", punishment.server, false)
                embed.addField("処罰解除者", "${unPunishOpProfile.name} (${unPunishOpProfile.uniqueId})", false)
                embed.addField("解除理由", reason, false)
                embed.addField("解除ID", id.toString(), false)
                embed.addField("処罰執行者", "${profile.name} (${profile.uniqueId})", false)
                embed.addField("被処罰者", punishment.name, false)
                embed.addField("処罰理由", punishment.reason, false)
                embed.addField("処罰ID", punishment.id.toString(), false)
                embed.addField("処罰日時", SABMessages.formatDate(punishment.start), false)
                if (punishment.type.name.contains("TEMP")) {
                    embed.addField("期間", Util.unProcessTime(punishment.end.serializeAsLong() - punishment.start), false)
                    embed.addField("期限", if (punishment.end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(punishment.end.serializeAsLong()), false)
                }
                webhook.addEmbed(embed)
                webhook.execute()
            }.then {}.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    fun Proof.sendWebhook(actor: Actor, content: String, color: Color? = null): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(punishment.server, punishment.type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return async<Unit> { context ->
                webhook.username = actor.name
                webhook.content = content
                val embed = DiscordWebhook.EmbedObject()
                embed.color = color
                embed.addField("種類", "${punishment.type.getName()} (${punishment.type.name})", false)
                embed.addField("コマンド実行者", "${actor.name} (${actor.uniqueId})", false)
                embed.addField("対象サーバー", punishment.server, false)
                embed.addField("証拠テキスト", text, false)
                embed.addField("証拠ID", id.toString(), false)
                embed.addField("被処罰者", punishment.name, false)
                embed.addField("処罰ID", punishment.id.toString(), false)
                embed.addField("処罰理由", punishment.reason, false)
                if (text.startsWith("https://") &&
                    !text.contains(" ") &&
                    (text.endsWith(".png", true) || text.endsWith(".jpg", true) || text.endsWith(".gif", true))
                ) {
                    embed.image = DiscordWebhook.EmbedObject.Image(text)
                }
                webhook.addEmbed(embed)
                try {
                    webhook.execute()
                    context.resolve()
                } catch (e: Throwable) {
                    context.reject(e)
                }
            }.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }
}
