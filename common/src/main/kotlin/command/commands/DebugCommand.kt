package dev.sentix.squaremarker.command.commands

import dev.sentix.squaremarker.Components
import dev.sentix.squaremarker.SquareMarker
import dev.sentix.squaremarker.command.Commander
import dev.sentix.squaremarker.command.Commands
import dev.sentix.squaremarker.command.SquaremarkerCommand
import org.incendo.cloud.component.DefaultValue
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.minecraft.extras.RichDescription.richDescription
import org.incendo.cloud.parser.standard.StringParser.greedyStringParser
import java.io.File

class DebugCommand(
    plugin: SquareMarker,
    commands: Commands,
) : SquaremarkerCommand(
        plugin,
        commands,
    ) {
    override fun register() {
        commands.registerSubcommand { builder ->
            builder
                .literal("debug")
                .optional("path", greedyStringParser(), DefaultValue.constant("."))
                .commandDescription(richDescription(Components.parse("Debug file paths and list files.")))
                .permission("squaremarker.debug")
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

    private fun execute(context: CommandContext<Commander>) {
        val sender = context.sender()
        val inputPath: String = context.get("path")

        val file = resolveFilePath(inputPath)

        Components.sendPrefixed(sender, "<gray>═══════════════════════════════════════")
        Components.sendPrefixed(sender, "<color:#8411FB>Debug Info")
        Components.sendPrefixed(sender, "<gray>Plugin data dir: <white>${squareMarker.dataDir.toAbsolutePath()}")
        Components.sendPrefixed(sender, "<gray>Input path: <white>$inputPath")
        Components.sendPrefixed(sender, "<gray>Resolved to: <white>${file.absolutePath}")
        Components.sendPrefixed(sender, "<gray>Exists: <white>${file.exists()}")
        Components.sendPrefixed(sender, "<gray>Is file: <white>${file.isFile}")
        Components.sendPrefixed(sender, "<gray>Is directory: <white>${file.isDirectory}")
        Components.sendPrefixed(sender, "<gray>Can read: <white>${file.canRead()}")
        Components.sendPrefixed(sender, "<gray>Can write: <white>${file.canWrite()}")

        if (file.isDirectory) {
            Components.sendPrefixed(sender, "<gray>─────────────────────────────────────")
            Components.sendPrefixed(sender, "<color:#8411FB>Files in directory:")

            val files = file.listFiles()
            if (files != null && files.isNotEmpty()) {
                files
                    .sortedBy { it.name }
                    .forEach { child ->
                        val type =
                            when {
                                child.isDirectory -> "<color:#FFD700>[DIR]"
                                child.name.endsWith(".png", true) -> "<color:#00FF00>[PNG]"
                                child.name.endsWith(".jpg", true) || child.name.endsWith(".jpeg", true) -> "<color:#00FF00>[JPG]"
                                else -> "<color:#808080>[FILE]"
                            }
                        Components.sendPrefixed(sender, "$type <white>${child.name}")
                    }
                Components.sendPrefixed(sender, "<gray>Total: <white>${files.size} items")
            } else {
                Components.sendPrefixed(sender, "<gray>Directory is empty or cannot be read")
            }
        }

        Components.sendPrefixed(sender, "<gray>═══════════════════════════════════════")
    }
}
