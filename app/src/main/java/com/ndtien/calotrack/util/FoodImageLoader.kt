package com.ndtien.calotrack.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.text.Normalizer

/**
 * Bộ tải ảnh thực phẩm từ thư mục `assets/foods/` kèm bộ nhớ đệm LruCache:
 * - Khớp chính xác theo slug tên món (VD: "Phở bò" -> "pho_bo.jpg")
 * - Nếu người dùng thêm món mới hoặc nhập nhanh, tự động khớp gần đúng theo từ khóa món
 *   hoặc rơi về ảnh đại diện theo nhóm món (category fallback).
 */
object FoodImageLoader {

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    private val bitmapCache = object : LruCache<String, Bitmap>(96) {}
    @Volatile
    private var availableAssets: Set<String>? = null

    fun slugOf(name: String): String =
        Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase()
            .replace(NON_ALNUM, "_")
            .trim('_')

    private fun getAvailableSet(context: Context): Set<String> {
        availableAssets?.let { return it }
        val set = runCatching {
            context.assets.list("foods")?.toSet().orEmpty()
        }.getOrDefault(emptySet())
        availableAssets = set
        return set
    }

    private fun resolveAssetFile(context: Context, foodName: String, category: String?): String {
        val files = getAvailableSet(context)
        val slug = slugOf(foodName)
        val exact = "$slug.jpg"
        if (exact in files) return exact

        // Khớp gần đúng: ưu tiên file có tên dài nhất nằm trong slug hoặc chứa slug
        if (slug.isNotEmpty()) {
            var bestMatch: String? = null
            var bestLen = 0
            for (f in files) {
                val stem = f.removeSuffix(".jpg")
                if (stem.length >= 3 && (slug.contains(stem) || stem.contains(slug))) {
                    if (stem.length > bestLen) {
                        bestLen = stem.length
                        bestMatch = f
                    }
                }
            }
            if (bestMatch != null) return bestMatch
        }

        // Ảnh đại diện theo nhóm thực phẩm
        val catFallback = when (category) {
            "Tinh bột" -> "com_trang.jpg"
            "Món chính" -> "pho_bo.jpg"
            "Chất đạm" -> "thit_bo.jpg"
            "Rau củ" -> "salad_tron.jpg"
            "Món ăn nhẹ" -> "goi_cuon.jpg"
            "Trái cây" -> "tao.jpg"
            "Đồ uống" -> "ca_phe_sua.jpg"
            "Bánh kẹo" -> "banh_flan.jpg"
            else -> "pho_bo.jpg"
        }
        return if (catFallback in files) catFallback else files.firstOrNull() ?: exact
    }

    fun loadInto(imageView: ImageView, foodName: String, category: String? = null) {
        val context = imageView.context
        val fileName = resolveAssetFile(context, foodName, category)
        bitmapCache.get(fileName)?.let { cached ->
            imageView.setImageBitmap(cached)
            return
        }
        val bmp = runCatching {
            context.assets.open("foods/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
        if (bmp != null) {
            bitmapCache.put(fileName, bmp)
            imageView.setImageBitmap(bmp)
        }
    }
}

