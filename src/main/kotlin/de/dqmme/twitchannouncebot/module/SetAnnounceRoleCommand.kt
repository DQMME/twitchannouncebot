package de.dqmme.twitchannouncebot.module

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import dev.kord.common.kColor
import dev.kord.rest.builder.message.create.embed
import dev.schlaubi.mikbot.plugin.api.settings.guildAdminOnly
import dev.schlaubi.mikbot.plugin.api.util.safeGuild
import java.awt.Color

suspend fun Extension.setAnnounceRoleCommand() = ephemeralSlashCommand(::SetAnnounceRoleArguments) {
    name = "set-announce-role"
    description = "commands.set_announce_role.description"
    guildAdminOnly()

    action {
        val oldSettings = Database.settings.findOneById(safeGuild.id) ?: AnnouncerSettings(safeGuild.asGuild())
        val role = arguments.role

        if (role == null) {
            Database.settings.save(oldSettings.copy(announceRoleId = null))
            respond {
                embed {
                    title = translate("commands.set_announce_role.reset.title")
                    description = translate("commands.set_announce_role.reset.description")
                    color = Color.red.kColor
                }
            }
            return@action
        }

        Database.settings.save(oldSettings.copy(announceRoleId = role.id))
        respond {
            embed {
                title = translate("commands.set_announce_role.set.title")
                description = translate("commands.set_announce_role.set.description")
                color = Color.green.kColor
            }
        }
    }
}

class SetAnnounceRoleArguments : Arguments() {
    val role by optionalRole {
        name = "role"
        description = "commands.set_announce_role.arguments.role.description"
    }
}