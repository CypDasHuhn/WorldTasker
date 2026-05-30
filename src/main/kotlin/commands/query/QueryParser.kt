package dev.cypdashuhn.worldtasker.commands.query

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class TimeFilter(
    val type: TimeType,
    val operator: TimeOperator,
    val date: LocalDate
)

enum class TimeType { CREATED, WORKED, COMPLETED }

enum class TimeOperator { BEFORE, AFTER, ON }

data class TodoQuery(
    val nearRadius: Int? = null,
    val tags: String? = null,
    val name: String? = null,
    val author: String? = null,
    val timeFilter: TimeFilter? = null
)

object QueryParser {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(raw: String): TodoQuery {
        val tokens = tokenize(raw)
        var nearRadius: Int? = null
        var tags: String? = null
        var name: String? = null
        var author: String? = null
        var timeFilter: TimeFilter? = null

        var i = 0
        while (i < tokens.size) {
            when (tokens[i]) {
                "--near" -> {
                    i++
                    if (i >= tokens.size) throw QueryParseException("--near requires a chunk radius value")
                    nearRadius = tokens[i].toIntOrNull()
                        ?: throw QueryParseException("--near value must be an integer, got: ${tokens[i]}")
                }
                "--tags" -> {
                    i++
                    if (i >= tokens.size) throw QueryParseException("--tags requires a tag query value")
                    tags = tokens[i]
                }
                "--name" -> {
                    i++
                    if (i >= tokens.size) throw QueryParseException("--name requires a name value")
                    name = tokens[i]
                }
                "--author" -> {
                    i++
                    if (i >= tokens.size) throw QueryParseException("--author requires an author name")
                    author = tokens[i]
                }
                "--time" -> {
                    i++
                    if (i + 2 >= tokens.size) throw QueryParseException("--time requires <type> <operator> <date>")
                    val type = parseTimeType(tokens[i])
                    val op = parseTimeOperator(tokens[i + 1])
                    val date = parseDate(tokens[i + 2])
                    timeFilter = TimeFilter(type, op, date)
                    i += 2
                }
            }
            i++
        }

        return TodoQuery(nearRadius, tags, name, author, timeFilter)
    }

    private fun tokenize(raw: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < raw.length) {
            when {
                raw[i] == '"' -> {
                    val end = raw.indexOf('"', i + 1)
                    if (end == -1) throw QueryParseException("Unclosed quote")
                    tokens.add(raw.substring(i + 1, end))
                    i = end + 1
                }
                raw[i].isWhitespace() -> i++
                else -> {
                    val start = i
                    while (i < raw.length && !raw[i].isWhitespace()) i++
                    tokens.add(raw.substring(start, i))
                }
            }
        }
        return tokens
    }

    private fun parseTimeType(value: String): TimeType = when (value.lowercase()) {
        "created" -> TimeType.CREATED
        "worked" -> TimeType.WORKED
        "completed" -> TimeType.COMPLETED
        else -> TimeType.valueOf(value.uppercase())
    }

    private fun parseTimeOperator(value: String): TimeOperator = when (value.lowercase()) {
        "before", "<" -> TimeOperator.BEFORE
        "after", ">" -> TimeOperator.AFTER
        "on", "=" -> TimeOperator.ON
        else -> TimeOperator.valueOf(value.uppercase())
    }

    private fun parseDate(value: String): LocalDate {
        return try {
            LocalDate.parse(value, dateFormat)
        } catch (e: DateTimeParseException) {
            throw QueryParseException("Invalid date format: $value. Expected yyyy-MM-dd")
        }
    }
}

class QueryParseException(message: String) : RuntimeException(message)
