package de.dqmme.twitchannouncebot.server

import dev.schlaubi.mikbot.util_plugins.ktor.api.KtorExtensionPoint
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.pf4j.Extension

val previewImages = mutableMapOf<String, ByteArray>()

@Extension
class LiveImageServer : KtorExtensionPoint {
    override fun Application.apply() {
        routing {
            get("/twitch/live-image") {
                val img = call.parameters["image"] ?: return@get
                val byteArray = previewImages[img.split("-")[0]]
                if (byteArray != null) call.respond(byteArray)
            }
        }
    }
}