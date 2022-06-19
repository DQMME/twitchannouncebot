package de.dqmme.twitchannouncebot.database

import dev.kord.common.Locale
import dev.kord.common.entity.Snowflake
import dev.kord.common.kLocale
import dev.kord.core.entity.Guild
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun AnnouncerSettings(guild: Guild): AnnouncerSettings =
    AnnouncerSettings(guildId = guild.id, language = guild.preferredLocale.kLocale)

@Serializable
data class AnnouncerSettings(
    @SerialName("_id") val guildId: Snowflake,
    @SerialName("twitch_channel") val twitchChannel: String? = null,
    @SerialName("announce_channel_id") val announceChannelId: Snowflake? = null,
    @SerialName("go_live_message_id") val goLiveMessageId: Snowflake? = null,
    @SerialName("announce_role_id") val announceRoleId: Snowflake? = null,
    val language: Locale = Locale.ENGLISH_UNITED_STATES
)
