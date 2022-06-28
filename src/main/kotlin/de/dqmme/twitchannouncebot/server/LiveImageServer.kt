package de.dqmme.twitchannouncebot.server

import dev.schlaubi.mikbot.util_plugins.ktor.api.KtorExtensionPoint
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.routing
import io.ktor.server.resources.get
import io.ktor.server.response.respondBytes
import kotlinx.serialization.Serializable
import org.pf4j.Extension

val previewImages = mutableMapOf<String, ByteArray>()

@Serializable
@Resource("/twitch/live-image")
data class LiveImage(val image: String = "null")

@Extension
class LiveImageServer : KtorExtensionPoint {
    override fun Application.apply() {
        routing {
            get<LiveImage> { (image) ->
                val byteArray = previewImages[image.substringBefore('-')]
                if (byteArray != null) call.respondBytes(byteArray)
            }
        }
    }
}