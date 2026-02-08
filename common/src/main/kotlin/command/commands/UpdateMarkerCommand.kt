package dev.sentix.squaremarker.command.commands

import dev.sentix.squaremarker.Components
import dev.sentix.squaremarker.SquareMarker
import dev.sentix.squaremarker.command.Commands
import dev.sentix.squaremarker.command.PlayerCommander
import dev.sentix.squaremarker.command.SquaremarkerCommand
import dev.sentix.squaremarker.marker.Marker
import dev.sentix.squaremarker.marker.MarkerService
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.minecraft.extras.RichDescription.richDescription
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import xyz.jpenilla.squaremap.api.Key
import xyz.jpenilla.squaremap.api.SquaremapProvider
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

class UpdateMarkerCommand(
    plugin: SquareMarker,
    commands: Commands,
) : SquaremarkerCommand(
        plugin,
        commands,
    ) {
    override fun register() {
        commands.registerSubcommand { builder ->
            builder
                .literal("update")
                .required("id", integerParser())
                .optional("input", greedyStringParser(), DefaultValue.constant(" "))
                .commandDescription(richDescription(Components.parse("Update a marker to your position.")))
                .permission("squaremarker.set")
                .senderType(PlayerCommander::class.java)
                .handler(::execute)
        }
    }

    private fun resolveFilePath(path: String): File {
        val file = File(path)
        // If it's already absolute or starts with common prefixes, use as-is
        if (file.isAbsolute || path.startsWith("plugins/") || path.startsWith("./") || path.startsWith("../")) {
            return file
        }
        // Otherwise, resolve relative to plugin data folder
        return squareMarker.dataDir.resolve(path).toFile()
    }

    private fun execute(context: CommandContext<PlayerCommander>) {
        val sender = context.sender()

        val id: Int = context.get("id")

        val iconKey = "squaremarker_marker_icon_$id"

        val input: String = context.get("input")

        var content = ""
        var iconUrl = ""

        // Split input to extract content and icon URL/path
        val trimmedInput = input.trim()
        if (trimmedInput.isNotBlank()) {
            // Check if input contains a URL or file path
            val lastSpaceIndex = trimmedInput.lastIndexOf(' ')
            if (lastSpaceIndex > 0) {
                val potentialPath = trimmedInput.substring(lastSpaceIndex + 1)
                // If last token looks like a URL or file path, treat it as iconUrl
                if (potentialPath.startsWith("http://") ||
                    potentialPath.startsWith("https://") ||
                    potentialPath.startsWith("./") ||
                    potentialPath.startsWith("../") ||
                    potentialPath.startsWith("plugins/") ||
                    potentialPath.endsWith(".png", ignoreCase = true) ||
                    potentialPath.endsWith(".jpg", ignoreCase = true) ||
                    potentialPath.endsWith(".jpeg", ignoreCase = true)
                ) {
                    content = trimmedInput.substring(0, lastSpaceIndex).trim()
                    iconUrl = potentialPath
                } else {
                    content = trimmedInput
                }
            } else {
                // No space - check if entire input is a path/URL
                if (trimmedInput.startsWith("http://") ||
                    trimmedInput.startsWith("https://") ||
                    trimmedInput.startsWith("./") ||
                    trimmedInput.startsWith("../") ||
                    trimmedInput.startsWith("plugins/") ||
                    trimmedInput.endsWith(".png", ignoreCase = true) ||
                    trimmedInput.endsWith(".jpg", ignoreCase = true) ||
                    trimmedInput.endsWith(".jpeg", ignoreCase = true)
                ) {
                    iconUrl = trimmedInput
                } else {
                    content = trimmedInput
                }
            }
        }

        val marker =
            Marker(
                id,
                content,
                iconUrl,
                iconKey,
                sender.world,
                sender.x,
                sender.y,
                sender.z,
            )

        if (MarkerService.markerExist(id)) {
            if (MarkerService.getMarker(id).iconUrl.isNotBlank()) {
                SquaremapProvider.get().iconRegistry().unregister(Key.key(marker.iconKey))
                File("${SquaremapProvider.get().webDir()}/images/icon/registered/${marker.iconKey}.png").delete()
            }

            try {
                val image =
                    if (marker.iconUrl.startsWith("http://") || marker.iconUrl.startsWith("https://")) {
                        // Load from URL
                        ImageIO.read(URI(marker.iconUrl).toURL())
                    } else {
                        // Load from disk
                        val file = resolveFilePath(marker.iconUrl)
                        if (file.exists() && file.isFile) {
                            ImageIO.read(file)
                        } else {
                            throw IllegalArgumentException("File not found: ${file.absolutePath}")
                        }
                    }

                SquaremapProvider.get().iconRegistry().register(
                    Key.key(marker.iconKey),
                    image,
                )
            } catch (ex: Exception) {
                Components.sendPrefixed(sender, "<gray>Marker icon set to default. Error: ${ex.message}")
            }

            MarkerService.updateMarker(marker)
            Components.sendPrefixed(sender, "<gray>Updated existing marker with ID <color:#8411FB>$id<gray>.</gray>")
        } else {
            Components.sendPrefixed(sender, "<gray>No marker with ID <color:#8411FB>$id <gray>found.</gray>")
        }
    }
}
