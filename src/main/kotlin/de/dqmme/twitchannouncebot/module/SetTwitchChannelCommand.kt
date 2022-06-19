package de.dqmme.twitchannouncebot.module

import com.github.twitch4j.TwitchClient
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import de.dqmme.twitchannouncebot.util.executeAsync
import dev.kord.common.kColor
import dev.kord.rest.builder.message.create.embed
import dev.schlaubi.mikbot.plugin.api.settings.guildAdminOnly
import dev.schlaubi.mikbot.plugin.api.util.safeGuild
import org.koin.core.component.inject
import java.awt.Color

suspend fun Extension.setTwitchChannelCommand() = ephemeralSlashCommand(::SetTwitchChannelArguments) {
    name = "set-twitch-channel"
    description = "commands.set_twitch_channel.description"
    guildAdminOnly()

    val twitchClient by inject<TwitchClient>()

    action {
        val oldSettings = Database.settings.findOneById(safeGuild.id) ?: AnnouncerSettings(safeGuild.asGuild())
        val channel = arguments.channel

        if (channel == null) {
            Database.settings.save(oldSettings.copy(twitchChannel = null))
            respond {
                embed {
                    title = translate("commands.set_twitch_channel.reset.title")
                    description = translate("commands.set_twitch_channel.reset.description")
                    color = Color.red.kColor
                }
            }
            return@action
        }

        val channelInformation = twitchClient.helix.getUsers(null, null, listOf(channel)).executeAsync().users.getOrNull(0)

        if(channelInformation == null) {
            respond {
                embed {
                    title = translate("generic.error")
                    description = "generic.channel_not_found"
                    color = Color.green.kColor
                }
            }
            return@action
        }

        twitchClient.clientHelper.enableStreamEventListener(channelInformation.login)

        Database.settings.save(oldSettings.copy(twitchChannel = channelInformation.id))
        respond {
            embed {
                title = translate("commands.set_twitch_channel.set.title")
                description = translate("commands.set_twitch_channel.set.description")
                color = Color.green.kColor
            }
        }
    }
}

class SetTwitchChannelArguments : Arguments() {
    val channel by optionalString {
        name = "channel"
        description = "commands.set_twitch_channel.arguments.channel.description"
    }
}