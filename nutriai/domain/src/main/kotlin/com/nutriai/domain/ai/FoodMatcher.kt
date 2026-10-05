package com.nutriai.domain.ai

import com.nutriai.domain.model.FoodReference
import java.text.Normalizer

/**
 * Empareja el nombre devuelto por la IA (o escrito por el usuario) con alimentos del catálogo.
 * Si no hay coincidencia suficiente, no se elige ninguno: el usuario decide.
 */
object FoodMatcher {
    const val MIN_SCORE = 0.5

    data class Match(val food: FoodReference, val score: Double)

    fun bestMatch(name: String, catalog: List<FoodReference>): Match? =
        rank(name, catalog).firstOrNull()?.takeIf { it.score >= MIN_SCORE }

    fun rank(query: String, catalog: List<FoodReference>, limit: Int = 10): List<Match> {
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return emptyList()
        return catalog
            .map { Match(it, score(queryTokens, it)) }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<Match> { it.score }.thenBy { it.food.name.length })
            .take(limit)
    }

    private fun score(queryTokens: List<String>, food: FoodReference): Double {
        val query = queryTokens.joinToString(" ")
        return (listOf(food.name) + food.aliases).maxOf { candidate ->
            val candidateTokens = tokens(candidate)
            val normalized = candidateTokens.joinToString(" ")
            when {
                normalized == query -> 1.0
                candidateTokens.isEmpty() -> 0.0
                else -> {
                    val common = queryTokens.count { q -> candidateTokens.any { c -> c.startsWith(q) || q.startsWith(c) } }
                    val coverage = common.toDouble() / queryTokens.size
                    val precision = common.toDouble() / candidateTokens.size
                    0.9 * (0.7 * coverage + 0.3 * precision)
                }
            }
        }
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .trim()

    private val STOP_WORDS = setOf("de", "con", "y", "el", "la", "los", "las", "en", "al", "a", "of", "with", "and", "the")

    private fun tokens(text: String): List<String> =
        normalize(text).split(Regex("\\s+")).filter { it.length > 1 && it !in STOP_WORDS }
}
