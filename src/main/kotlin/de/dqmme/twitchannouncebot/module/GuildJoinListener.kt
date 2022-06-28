package de.dqmme.twitchannouncebot.module

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.core.event.guild.GuildCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take

suspend fun Extension.guildJoinListener() = event<GuildCreateEvent> {
    action {
        val permissions = Permissions(Permission.SendMessages, Permission.EmbedLinks, Permission.ReadMessageHistory)
        val channel = event.guild.channels.filterIsInstance<TopGuildMessageChannel>().filter {
            permissions in it.getEffectivePermissions(kord.selfId)
        }.take(1).singleOrNull()
        channel?.createEmbed {
            title = ""
        }
    }
}