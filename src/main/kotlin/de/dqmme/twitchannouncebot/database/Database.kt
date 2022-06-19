package de.dqmme.twitchannouncebot.database

import dev.schlaubi.mikbot.plugin.api.io.getCollection
import dev.schlaubi.mikbot.plugin.api.util.database
import org.koin.core.component.KoinComponent

object Database: KoinComponent {
    val settings = database.getCollection<AnnouncerSettings>("twitch_guild_announcer_settings")
}