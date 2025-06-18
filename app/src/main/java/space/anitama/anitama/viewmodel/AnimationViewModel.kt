package space.anitama.anitama.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.anitama.anitama.data.model.AnimationPack

class AnimationViewModel : ViewModel() {
    private val _animationPacks = MutableStateFlow<List<AnimationPack>>(emptyList())
    val animationPacks: StateFlow<List<AnimationPack>> = _animationPacks.asStateFlow()
    private val client = OkHttpClient()

    // Giả sử lưu trữ cục bộ bằng một biến tạm (có thể thay bằng DataStore hoặc Room)
    private var cachedPacks: List<AnimationPack> = emptyList()

    init {
        _animationPacks.value = emptyList()
        // Khi ViewModel được khởi tạo, lấy dữ liệu đã lưu và gọi API
        loadCachedData()
        fetchAnimationPacks()
    }

    private fun loadCachedData() {
        try {
            _animationPacks.value = cachedPacks
        } catch (e: Exception) {
            Log.e("AnimationViewModel", "Error loading cached data", e)
        }
    }

    fun fetchAnimationPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://anitama.space/anitama_list_model")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body?.string()
                        Log.w("AnimationViewModel", "API response: $json")
                        if (json.isNullOrEmpty()) {
                            Log.w("AnimationViewModel", "Empty response body")
                            _animationPacks.value = emptyList()
                            return@use
                        }
                        try {
                            val packs = Json { ignoreUnknownKeys = true }
                                .decodeFromString<List<AnimationPack>>(json)
                            _animationPacks.value = packs
                            cachedPacks = packs // Lưu vào bộ nhớ cục bộ
                            Log.d("AnimationViewModel", "Loaded ${packs.size} packs")
                        } catch (e: Exception) {
                            Log.e("AnimationViewModel", "Error parsing JSON", e)
                            _animationPacks.value = cachedPacks // Dùng cache nếu lỗi JSON
                        }
                    } else {
                        Log.e("AnimationViewModel", "API error: ${response.code}")
                        _animationPacks.value = cachedPacks // Dùng cache nếu API lỗi
                    }
                }
            } catch (e: IOException) {
                Log.e("AnimationViewModel", "Network error", e)
                _animationPacks.value = cachedPacks // Dùng cache nếu lỗi mạng
            } catch (e: Exception) {
                Log.e("AnimationViewModel", "Unexpected error", e)
                _animationPacks.value = cachedPacks // Dùng cache nếu lỗi khác
            }
        }
    }
}
