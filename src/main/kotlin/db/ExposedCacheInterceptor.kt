package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.GlobalStatementInterceptor
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementType

private val TableModifiedKey = Key<Boolean>()

object ExposedCacheInterceptor : GlobalStatementInterceptor {
    override fun beforeExecution(transaction: Transaction, context: StatementContext) {
        if (context.statement.type in listOf(StatementType.INSERT, StatementType.UPDATE, StatementType.DELETE)) {
            transaction.putUserData(TableModifiedKey, true)
        }
    }

    override fun beforeCommit(transaction: Transaction) {
        if (transaction.getUserData(TableModifiedKey) == true) {
            SuggestionCache.invalidateAll()
        }
    }
}

fun ensureExposedInterceptorRegistered() {
    try {
        val companionField = Transaction::class.java.getDeclaredField("Companion")
        companionField.isAccessible = true
        val companion = companionField.get(null)
        val interceptorsField = companion.javaClass.getDeclaredField("globalInterceptors")
        interceptorsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val interceptors = interceptorsField.get(companion) as MutableList<GlobalStatementInterceptor>
        if (ExposedCacheInterceptor !in interceptors) {
            interceptors.add(ExposedCacheInterceptor)
        }
    } catch (_: Exception) {
    }
}
