package dev.cypdashuhn.worldtasker.commands.query

sealed class TagExpr {
    data class Leaf(
        val name: String
    ) : TagExpr()

    data class And(
        val left: TagExpr,
        val right: TagExpr
    ) : TagExpr()

    data class Or(
        val left: TagExpr,
        val right: TagExpr
    ) : TagExpr()

    data class Not(
        val expr: TagExpr
    ) : TagExpr()

    fun matches(tagNames: Set<String>): Boolean =
        when (this) {
            is Leaf -> name in tagNames
            is And -> left.matches(tagNames) && right.matches(tagNames)
            is Or -> left.matches(tagNames) || right.matches(tagNames)
            is Not -> !expr.matches(tagNames)
        }
}

fun parseTagDsl(input: String): TagExpr = TagDslParser(tokenizeTagDsl(input)).parse()

private fun tokenizeTagDsl(input: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < input.length) {
        when {
            input[i].isWhitespace() -> {
                i++
            }

            input[i] in "(),+-" -> {
                tokens.add(input[i].toString())
                i++
            }

            else -> {
                val start = i
                while (i < input.length && !input[i].isWhitespace() && input[i] !in "(),+") i++
                tokens.add(input.substring(start, i))
            }
        }
    }
    return tokens
}

private class TagDslParser(
    private val tokens: List<String>
) {
    private var pos = 0

    fun parse(): TagExpr {
        val expr = parseOr()
        if (pos < tokens.size) throw IllegalArgumentException("Unexpected token: '${tokens[pos]}'")
        return expr
    }

    // lowest precedence: comma = OR
    private fun parseOr(): TagExpr {
        var left = parseAnd()
        while (pos < tokens.size && tokens[pos] == ",") {
            pos++
            left = TagExpr.Or(left, parseAnd())
        }
        return left
    }

    // middle precedence: plus = AND
    private fun parseAnd(): TagExpr {
        var left = parseNot()
        while (pos < tokens.size && tokens[pos] == "+") {
            pos++
            left = TagExpr.And(left, parseNot())
        }
        return left
    }

    // unary prefix: minus = NOT
    private fun parseNot(): TagExpr {
        if (pos < tokens.size && tokens[pos] == "-") {
            pos++
            return TagExpr.Not(parseAtom())
        }
        return parseAtom()
    }

    private fun parseAtom(): TagExpr {
        if (pos >= tokens.size) throw IllegalArgumentException("Unexpected end of tag expression")
        if (tokens[pos] == "(") {
            pos++
            val expr = parseOr()
            if (pos >= tokens.size || tokens[pos] != ")") {
                throw IllegalArgumentException("Expected ')'")
            }
            pos++
            return expr
        }
        return TagExpr.Leaf(tokens[pos++])
    }
}
