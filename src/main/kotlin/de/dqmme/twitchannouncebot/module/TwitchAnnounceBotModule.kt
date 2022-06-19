package de.dqmme.twitchannouncebot.module

import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.events.ChannelChangeGameEvent
import com.github.twitch4j.events.ChannelChangeTitleEvent
import com.github.twitch4j.events.ChannelGoLiveEvent
import com.github.twitch4j.events.ChannelGoOfflineEvent
import com.github.twitch4j.helix.domain.User
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import de.dqmme.twitchannouncebot.database.AnnouncerSettings
import de.dqmme.twitchannouncebot.database.Database
import de.dqmme.twitchannouncebot.server.previewImages
import de.dqmme.twitchannouncebot.util.executeAsync
import de.dqmme.twitchannouncebot.util.getCurrentStream
import de.dqmme.twitchannouncebot.util.getEventHandler
import de.dqmme.twitchannouncebot.util.getSingleUser
import de.dqmme.twitchannouncebot.util.onEvent
import de.dqmme.twitchannouncebot.util.twitchPreviewUrl
import de.dqmme.twitchannouncebot.util.uptime
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.schlaubi.mikbot.plugin.api.pluginSystem
import dev.schlaubi.mikbot.plugin.api.util.AllShardsReadyEvent
import dev.schlaubi.mikbot.plugin.api.util.embed
import dev.schlaubi.mikbot.util_plugins.ktor.api.buildBotUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.not
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

private val LOG = KotlinLogging.logger { }

@OptIn(KordUnsafe::class, KordExperimental::class)
class TwitchAnnounceBotModule : Extension() {
    override val name: String = "twitchannouncebot"
    override val bundle: String = "twitchannouncebot"
    private val twitchClient by inject<TwitchClient>()

    @OptIn(ObsoleteCoroutinesApi::class)
    private val ticker = ticker(60.seconds.inWholeMilliseconds, 0)
    private lateinit var runner: Job
    private val client = HttpClient()

    override suspend fun setup() {
        setAnnounceChannelCommand()
        setAnnounceRoleCommand()
        setTwitchChannelCommand()

        event<AllShardsReadyEvent> {
            action {
                updateEmbeds()
                listenForEvents()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            embeddedServer(Netty, port = 7953) {

            }.start(wait = true)
        }
    }


    private suspend fun listenForEvents() {
        val channelIds = Database.settings.find().toFlow().map { it.twitchChannel }.toList()

        if (channelIds.isNotEmpty()) {
            val userIds = twitchClient.helix.getUsers(null, channelIds, null)
                .executeAsync()
                .users
                .map { it.login }

            LOG.debug { "Registering subscriptions" }
            twitchClient.clientHelper.enableStreamEventListener(userIds)
        }

        val eventHandler = twitchClient.eventManager.getEventHandler<SimpleEventHandler>()

        eventHandler.onEvent<ChannelGoLiveEvent> { kord.launch { onChannelGoLiveEvent(it) } }
        eventHandler.onEvent<ChannelChangeTitleEvent> { kord.launch { onChangeTitleEvent(it) } }
        eventHandler.onEvent<ChannelChangeGameEvent> { kord.launch { onChangeGameEvent(it) } }
        eventHandler.onEvent<ChannelGoOfflineEvent> { kord.launch { onChannelGoOfflineEvent(it) } }

        update()
    }

    private suspend fun update() {
        runner = coroutineScope {
            launch {
                for (unit in ticker) {
                    forAllChannels {
                        val stream = twitchClient.helix.getCurrentStream(channelId) ?: return@forAllChannels
                        val response = client.get(twitchPreviewUrl(user.login))
                        val url: String?

                        if (response.status.isSuccess()) {
                            previewImages[channelId] = response.body()
                            url = buildBotUrl {
                                appendPathSegments("twtich", "live-image")
                                parameters["image"] = "$channelId-${System.currentTimeMillis()}"
                            }.toString()
                        } else {
                            url = twitchPreviewUrl(user.login)
                        }

                        updateStream(
                            channelId,
                            user.login,
                            stream.gameName,
                            stream.title,
                            stream.viewerCount,
                            previewUrl = url,
                            uptime = stream.uptime()
                        )
                    }
                }
            }
        }
    }

    private suspend fun onChannelGoOfflineEvent(channelGoOfflineEvent: ChannelGoOfflineEvent) {
        LOG.debug { "Received go offline event: $channelGoOfflineEvent" }
        updateStreamOffline(channelGoOfflineEvent.channel.id, channelGoOfflineEvent.channel.name)
    }

    private suspend fun onChangeTitleEvent(changeTitleEvent: ChannelChangeTitleEvent) {
        LOG.debug { "Received change title event: $changeTitleEvent" }

        updateStream(
            changeTitleEvent.channel.id,
            changeTitleEvent.channel.name,
            changeTitleEvent.stream.gameName,
            changeTitleEvent.stream.title,
            changeTitleEvent.stream.viewerCount
        )
    }

    private suspend fun onChangeGameEvent(changeTitleEvent: ChannelChangeGameEvent) {
        LOG.debug { "Received change game event: $changeTitleEvent" }

        updateStream(
            changeTitleEvent.channel.id,
            changeTitleEvent.channel.name,
            changeTitleEvent.stream.gameName,
            changeTitleEvent.stream.title,
            changeTitleEvent.stream.viewerCount
        )
    }

    private suspend fun onChannelGoLiveEvent(channelGoLiveEvent: ChannelGoLiveEvent) {
        LOG.debug { "Received live event: $channelGoLiveEvent" }

        updateStream(
            channelGoLiveEvent.channel.id,
            channelGoLiveEvent.channel.name,
            channelGoLiveEvent.stream.gameName,
            channelGoLiveEvent.stream.title,
            channelGoLiveEvent.stream.viewerCount,
            true
        )
    }

    private suspend fun updateEmbeds() {
        forAllChannelsNothingNull {
            if (kord.unsafe.guildMessageChannel(settings.guildId, settings.announceChannelId!!)
                    .getMessageNull(settings.goLiveMessageId!!) == null
            ) return@forAllChannelsNothingNull

            val stream = twitchClient.helix.getCurrentStream(channelId)

            if (stream == null || stream.type != "live") {
                updateStreamOffline(channelId, user.login)
                return@forAllChannelsNothingNull
            }

            updateStream(
                channelId,
                user.login,
                stream.gameName,
                stream.title,
                stream.viewerCount,
                uptime = stream.uptime()
            )
        }
    }

    private suspend fun updateStreamOffline(channelId: String, channelName: String) {
        forAllChannels(channelId) {
            val channelInformation = twitchClient.helix.getSingleUser(channelId)

            val offlineEmbed = embed {
                this.title = settings.translate("steam.went_offline.title", channelName)

                description = settings.translate("steam.went_offline.description")

                thumbnail {
                    url = channelInformation.profileImageUrl
                }

                image = channelInformation.offlineImageUrl
            }

            val message = kord.unsafe.guildMessageChannel(settings.guildId, settings.announceChannelId!!).withStrategy(
                EntitySupplyStrategy.rest
            )
                .getMessageNull(settings.goLiveMessageId!!)

            message?.edit {
                embeds = mutableListOf(offlineEmbed)
            }

            Database.settings.save(settings.copy(goLiveMessageId = null))
        }
    }

    private suspend fun updateStream(
        channelId: String,
        channelName: String,
        categoryName: String,
        title: String,
        viewerCount: Int,
        deleteOldMessage: Boolean = false,
        previewUrl: String = twitchPreviewUrl(channelName),
        uptime: String = "0s"
    ) {
        forAllChannels(channelId) {
            if (deleteOldMessage) {
                settings.deleteOldMessage()
            }

            val channel = kord.unsafe.guildMessageChannel(settings.guildId, settings.announceChannelId!!)

            val embed = embed {
                this.title = settings.translate("steam.started.title", channelName)

                description = settings.translate("steam.started.description", channelName, categoryName)

                field {
                    name = settings.translate("stream.title.name")
                    value = title
                    inline = false
                }

                if (categoryName.isNotEmpty()) {
                    field {
                        name = settings.translate("steam.game.name")
                        value = categoryName
                        inline = true
                    }
                }

                field {
                    name = settings.translate("stream.status.name")
                    value = settings.translate("stream.status.description", viewerCount)
                    inline = true
                }

                field {
                    name = settings.translate("stream.uptime.name")
                    value = uptime
                    inline = true
                }

                author {
                    name = channelName
                    url = "https://twitch.tv/${channelName}"
                    icon = user.profileImageUrl
                }

                thumbnail {
                    url = user.profileImageUrl
                }

                image = previewUrl

                url = "https://twitch.tv/${channelName}"
            }

            if (settings.goLiveMessageId != null) {
                val message = channel.getMessageNull(settings.goLiveMessageId)
                if (message != null) {
                    message.edit {
                        embeds = mutableListOf(embed)
                    }
                    return@forAllChannels
                }
            }

            if (deleteOldMessage) {
                val newMessage = channel.createMessage {
                    if (settings.announceRoleId != null) content = "<@&${settings.announceRoleId}>"
                    embeds.add(embed)
                }

                Database.settings.save(settings.copy(goLiveMessageId = newMessage.id))
            }
        }
    }

    private data class ChannelContext(val channelId: String, val user: User, val settings: AnnouncerSettings)

    private suspend fun forAllChannels(channelId: String, onEach: suspend ChannelContext.() -> Unit) {
        val channels = Database.settings.find(
            and(
                AnnouncerSettings::twitchChannel eq channelId,
                not(AnnouncerSettings::announceChannelId eq null)
            )
        )

        val user = twitchClient.helix.getSingleUser(channelId)

        coroutineScope {
            channels
                .toFlow()
                .onEach { settings ->
                    onEach(ChannelContext(channelId, user, settings))
                }
                .launchIn(this)
        }
    }

    private suspend fun forAllChannels(onEach: suspend ChannelContext.() -> Unit) {
        val channels = Database.settings.find(
            and(
                not(AnnouncerSettings::twitchChannel eq null),
                not(AnnouncerSettings::announceChannelId eq null)
            )
        )

        coroutineScope {
            channels
                .toFlow()
                .onEach { settings ->
                    val user = twitchClient.helix.getSingleUser(settings.twitchChannel!!)
                    onEach(ChannelContext(settings.twitchChannel, user, settings))
                }
                .launchIn(this)
        }
    }

    private suspend fun forAllChannelsNothingNull(onEach: suspend ChannelContext.() -> Unit) {
        val channels = Database.settings.find(
            and(
                not(AnnouncerSettings::twitchChannel eq null),
                not(AnnouncerSettings::announceChannelId eq null),
                not(AnnouncerSettings::goLiveMessageId eq null)
            )
        )

        coroutineScope {
            channels
                .toFlow()
                .onEach { settings ->
                    val user = twitchClient.helix.getSingleUser(settings.twitchChannel!!)
                    onEach(ChannelContext(settings.twitchChannel, user, settings))
                }
                .launchIn(this)
        }
    }

    private suspend fun AnnouncerSettings.deleteOldMessage() = runCatching {
        Database.settings.save(copy(goLiveMessageId = null))
    }

    private suspend fun GuildMessageChannelBehavior.getMessageNull(id: Snowflake): Message? {
        return try {
            getMessage(id)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun AnnouncerSettings.translate(key: String, vararg arguments: Any): String = pluginSystem.translate(
        key, bundle, language.asJavaLocale().toLanguageTag(), replacements = arguments as Array<Any?>
    )
}
