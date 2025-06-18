package space.anitama.anitama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnimationPack(
    val index: Int,
    val modelName: String,
    val pngDemoLink: String,
    val pngBackgroundLink: String,
    val zipUrl: String
)

data class Animation(
    val name: String,
    val images: List<String>
)