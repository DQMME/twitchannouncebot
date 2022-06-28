package de.dqmme.twitchannouncebot.module

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import dev.kord.common.kColor
import dev.kord.common.kLocale
import dev.kord.rest.builder.message.create.embed
import dev.schlaubi.mikbot.plugin.api.util.safeGuild
import java.awt.Color
import java.util.Locale

enum class Language(val locale: Locale) : ChoiceEnum {
    GERMAN(Locale.GERMAN),
    ENGLISH(Locale.ENGLISH);

    override val readableName: String
        get() = locale.displayName
}

class SetLanguageArguments : Arguments() {
    val language by enumChoice<Language> {
        name = "language"
        typeName = "Language"
        description = "commands.language.arguments.language.description"
    }
}

suspend fun Extension.setLanguageCommand() = ephemeralSlashCommand(::SetLanguageArguments) {
    name = "language"
    description = "commands.language.description"

    action {
        val oldSettings = Database.settings.findOneById(safeGuild.id) ?: AnnouncerSettings(safeGuild.asGuild())
        val language = arguments.language.locale

        Database.settings.save(oldSettings.copy(language = language.kLocale))
        respond {
            embed {
                title = translate("commands.language.set.title")
                description = translate("commands.language.set.description", arrayOf(language.getDisplayName(language)))
                color = Color.green.kColor
            }
        }
    }
}