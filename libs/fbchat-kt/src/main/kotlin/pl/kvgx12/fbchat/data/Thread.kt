package pl.kvgx12.fbchat.data

import kotlinx.serialization.Serializable

const val DEFAULT_COLOR = "#0084ff"
val AVAILABLE_COLORS = setOf(
    DEFAULT_COLOR,
    "#44bec7",
    "#ffc300",
    "#fa3c4c",
    "#d696bb",
    "#6699cc",
    "#13cf13",
    "#ff7e29",
    "#e68585",
    "#7646ff",
    "#20cef5",
    "#67b868",
    "#d4a88c",
    "#ff5ca1",
    "#a695c7",
    "#ff7ca8",
    "#1adb5b",
    "#f01d6a",
    "#ff9c19",
    "#0edcde",
)
val genderMapping = mapOf<String?, _>(
    // For standard requests
    "0" to "unknown",
    "1" to "female_singular",
    "2" to "male_singular",
    "3" to "female_singular_guess",
    "4" to "male_singular_guess",
    "5" to "mixed",
    "6" to "neuter_singular",
    "7" to "unknown_singular",
    "8" to "female_plural",
    "9" to "male_plural",
    "10" to "neuter_plural",
    "11" to "unknown_plural",
    // For graphql requests
    "UNKNOWN" to "unknown",
    "FEMALE" to "female_singular",
    "MALE" to "male_singular",
    // '' to 'female_singular_guess',
    // '' to 'male_singular_guess',
    // '' to 'mixed',
    "NEUTER" to "neuter_singular",
    // '' to 'unknown_singular',
    // '' to 'female_plural',
    // '' to 'male_plural',
    // '' to 'neuter_plural',
    // '' to 'unknown_plural',
)

@Serializable
sealed interface Thread {
    val id: String
}

@Serializable
sealed interface ThreadData : Thread

@Serializable
sealed interface ThreadId : Thread

@Serializable
sealed interface Group : Thread

@Serializable
sealed interface User : Thread

@Serializable
sealed interface Page : Thread

@Serializable
data class UnknownThread(
    override val id: String,
) : ThreadId

@Serializable
data class GroupId(override val id: String) : Group, ThreadId

@Serializable
data class UserId(override val id: String) : User, ThreadId

@Serializable
data class PageId(override val id: String) : Page, ThreadId

@Serializable
data class GroupData(
    override val id: String,
    val photo: Image?,
    val name: String?,
    val lastActive: Long?,
    val messageCount: Int?,
    val participants: List<ThreadId>,
    val nicknames: Map<String, String>,
    val color: String?,
    val emoji: String?,
    val admins: List<String>,
    val approvalMode: Boolean?,
    val approvalRequests: List<String>,
    val joinLink: String?,
) : Group, ThreadData

@Serializable
data class UserData(
    override val id: String,
    val photo: Image,
    val name: String,
    val isFriend: Boolean,
    val firstName: String,
    val lastName: String?,
    val lastActive: Long?,
    val messageCount: Int?,
    val url: String?,
    val gender: String?,
    val affinity: Float?,
    val nickname: String?,
    val ownNickname: String?,
    val color: String?,
    val emoji: String?,
) : User, ThreadData

@Serializable
data class PageData(
    override val id: String,
    val photo: Image,
    val name: String,
    val lastActive: Long?,
    val messageCount: Int?,
    val url: String?,
    val city: String?,
    val likes: String?,
    val subtitle: String?,
    val category: String?,
) : Page, ThreadData
