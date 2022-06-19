package de.dqmme.twitchannouncebot.module

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import dev.kord.common.entity.ChannelType
import dev.kord.common.kColor
import dev.kord.rest.builder.message.create.embed
import dev.schlaubi.mikbot.plugin.api.settings.guildAdminOnly
import dev.schlaubi.mikbot.plugin.api.util.safeGuild
import java.awt.Color

suspend fun Extension.setAnnounceChannelCommand() = ephemeralSlashCommand(::SetAnnounceChannelArguments) {
    name = "set-announce-channel"
    description = "commands.set_announce_channel.description"
    guildAdminOnly()

    action {
        val oldSettings = Database.settings.findOneById(safeGuild.id) ?: AnnouncerSettings(safeGuild.asGuild())
        val channel = arguments.channel

        if (channel == null) {
            Database.settings.save(oldSettings.copy(announceChannelId = null))
            respond {
                embed {

                    title = translate("commands.set_announce_channel.reset.success.title")
                    description = translate("commands.set_announce_channel.reset.success.description")
                    color = Color.red.kColor
                }
            }
            return@action
        }

        Database.settings.save(oldSettings.copy(announceChannelId = channel.id))
        respond {
            embed {
                title = translate("commands.set_announce_channel.set.success.title")
                description = translate("commands.set_announce_channel.set.success.description")
                color = Color.green.kColor
            }
        }
    }
}

class SetAnnounceChannelArguments : Arguments() {
    val channel by optionalChannel {
        name = "channel"
        description = "commands.set_announce_channel.arguments.channel.description"
        requireChannelType(ChannelType.GuildText)
        requireChannelType(ChannelType.GuildNews)
    }
}