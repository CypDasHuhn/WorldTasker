package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import org.bukkit.entity.Player

private val VALID_NAME = Regex("[a-z0-9_\\-]+")

val RESERVED_NAMES = setOf("no-namespace")

fun isValidResourceName(name: String) = name.matches(VALID_NAME)

internal fun Player.reservedName(name: String) =
    msg("<red>'<white>$name</white>' is reserved and cannot be used.")
