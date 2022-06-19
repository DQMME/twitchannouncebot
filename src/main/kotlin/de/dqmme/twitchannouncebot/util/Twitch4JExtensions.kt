package de.dqmme.twitchannouncebot.util

import com.github.philippheuer.events4j.api.service.IEventHandler
import com.github.philippheuer.events4j.core.EventManager
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.domain.Stream
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant

inline fun <reified E> IEventHandler.onEvent(noinline handler: (E) -> Unit) = onEvent(E::class.java, handler)

inline fun <reified EH : IEventHandler> EventManager.getEventHandler() = getEventHandler(EH::class.java)

suspend fun TwitchHelix.getSingleUser(id: String) = getUsers(null, listOf(id), null).executeAsync().users.single()

suspend fun TwitchHelix.getSinglePublicChannel(id: String) =
    getChannelInformation(null, listOf(id)).executeAsync().channels.single()

suspend fun TwitchHelix.getCurrentStream(channelId: String): Stream? {
    val stream = getStreams(
        null,
        null,
        null,
        1,
        null,
        null,
        listOf(channelId),
        null
    ).executeAsync().streams.getOrNull(0) ?: return null

    if (stream.type != "live") return null

    return stream
}

fun twitchPreviewUrl(channelName: String) = "https://static-cdn.jtvnw.net/previews-ttv/live_user_${channelName}.jpg"

fun Stream.uptime() = startedAtInstant.toKotlinInstant().toMessageFormat(DiscordTimestampStyle.RelativeTime)
