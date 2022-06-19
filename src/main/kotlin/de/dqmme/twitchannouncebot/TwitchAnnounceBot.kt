package de.dqmme.twitchannouncebot

import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import de.dqmme.twitchannouncebot.config.Config
import de.dqmme.twitchannouncebot.module.TwitchAnnounceBotModule
import dev.schlaubi.mikbot.plugin.api.Plugin
import dev.schlaubi.mikbot.plugin.api.PluginMain
import dev.schlaubi.mikbot.plugin.api.PluginWrapper
import org.koin.dsl.module

@PluginMain
class TwitchAnnounceBot(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val twitchClient: TwitchClient = TwitchClientBuilder.builder()
        .withClientId(Config.TWITCH_CLIENT_ID)
        .withClientSecret(Config.TWITCH_CLIENT_SECRET)
        .withEnableHelix(true)
        .withEnablePubSub(true)
        .withEnableGraphQL(true)
        .build()


    override suspend fun ExtensibleBotBuilder.apply() {
        hooks {
            afterKoinSetup {
                getKoin().loadModules(
                    listOf(module { single { twitchClient } })
                )
            }
        }
    }

    override fun ExtensibleBotBuilder.ExtensionsBuilder.addExtensions() {
        add(::TwitchAnnounceBotModule)
    }
}