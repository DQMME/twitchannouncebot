package de.dqmme.twitchannouncebot.game

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import dev.kord.core.Kord
import dev.schlaubi.mikbot.core.game_animator.api.GameAnimatorExtensionPoint
import kotlinx.coroutines.flow.count
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.not
import org.pf4j.Extension

@Extension
class GameAnimatorExtension : GameAnimatorExtensionPoint, KordExKoinComponent {
    private val kord by inject<Kord>()

    override suspend fun String.replaceVariables(): String {
        val twitchChannelCount = Database.settings.countDocuments(not(AnnouncerSettings::twitchChannel eq null))
        val guildCount = kord.guilds.count()
        return replace("%channel_count%", "$twitchChannelCount").replace("%guild_count%", "$guildCount")
    }
}