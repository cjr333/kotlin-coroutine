package flow

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun log(msg: String) = println("${formatCurrent()} [${Thread.currentThread().name}] $msg")
fun log(msg: Int) = log(msg.toString())

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
private fun formatCurrent(): String = timeFormatter.format(LocalDateTime.now())
