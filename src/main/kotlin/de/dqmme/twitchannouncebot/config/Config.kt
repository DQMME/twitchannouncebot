package de.dqmme.twitchannouncebot.config

import dev.schlaubi.mikbot.plugin.api.EnvironmentConfig

object Config : EnvironmentConfig() {
    val TWITCH_CLIENT_ID by environment
    val TWITCH_CLIENT_SECRET by environment
}