package pl.kvgx12.downloadapi.platforms.reddit

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RedditListings = List<@Contextual RedditListing>
typealias RedditThings = List<@Contextual IsRedditThing>

object RedditThingType {
    const val COMMENT = "t1"
    const val ACCOUNT = "t2"
    const val LINK = "t3"
    const val MESSAGE = "t4"
    const val SUBREDDIT = "t5"
    const val AWARD = "t6"

    const val LISTING = "Listing"
    const val MORE = "more"
}

sealed interface RedditObject

sealed interface IsRedditThing : RedditObject {
    val id: String
    val name: String
}

@Serializable
@SerialName(RedditThingType.LISTING)
data class RedditListing(
    val dist: Int?,
    val geoFilter: String,
    val modhash: String,
    val before: String?,
    val after: String?,
    val children: RedditThings,
) : RedditObject

@Serializable
data class RedditThing(override val id: String, override val name: String) : IsRedditThing

@SerialName(RedditThingType.LINK)
@Serializable
data class RedditLink(
    val subreddit: String,
    val title: String,
    val subredditType: String,
    val mediaEmbed: Map<String, MediaEmbed> = emptyMap(),
    val authorFullname: String,
    val secureMedia: Map<String, SecureMedia> = emptyMap(),
    val isRedditMediaDomain: Boolean,
    val secureMediaEmbed: Map<String, SecureMediaEmbed> = emptyMap(),
    val score: Int,
    val domain: String,
    val over18: Boolean = false,
    val subredditId: String,
    val author: String,
    val media: Map<String, Media> = emptyMap(),
    val permalink: String,
    val url: String,
    val isVideo: Boolean,

    override val id: String,
    override val name: String,
) : IsRedditThing

@Serializable
@SerialName(RedditThingType.COMMENT)
data class RedditComment(
    override val id: String,
    override val name: String,
) : IsRedditThing

@Serializable
@SerialName(RedditThingType.MORE)
data class RedditMore(
    override val id: String,
    override val name: String,
) : IsRedditThing
