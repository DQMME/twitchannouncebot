package de.dqmme.twitchannouncebot.database

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.schlaubi.mikbot.plugin.api.io.getCollection
import dev.schlaubi.mikbot.plugin.api.util.database

object Database: KordExKoinComponent {
    val settings = database.getCollection<AnnouncerSettings>("twitch_guild_announcer_settings")
}