package hg2g.api

import se.ansman.kotshi.JsonSerializable

enum class Language {
    Universal, EarthSpeak
}

@JsonSerializable
data class Article(val language: Language, val text: String)