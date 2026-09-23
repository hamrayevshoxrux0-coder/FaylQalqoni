package uz.faylqalqoni.shield.service

import android.os.FileObserver
import android.util.Log
import java.io.File

/**
 * android.os.FileObserver faqat BITTA papkani kuzatadi, ichki papkalarni emas.
 * Bu klass berilgan papka ostidagi barcha papkalarga (chegaralangan chuqurlikda)
 * avtomatik ravishda alohida FileObserver o'rnatib, "rekursiv kuzatish" hosil qiladi.
 * Yangi papka yaratilsa, unga ham darhol kuzatuvchi qo'shiladi.
 */
class RecursiveFileObserver(
    private val root: File,
    private val maxDepth: Int = 4,
    private val onFileEvent: (File) -> Unit
) {
    // ~1000_0000 dan ("interesting" flag'lar): fayl yaratildi, yozish tugadi, ko'chirildi
    private val watchMask = FileObserver.CREATE or FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO

    private val observers = mutableListOf<FileObserver>()

    fun start() {
        stop()
        watchRecursively(root, 0)
    }

    fun stop() {
        for (o in observers) {
            try { o.stopWatching() } catch (e: Exception) { /* e'tiborsiz qoldiriladi */ }
        }
        observers.clear()
    }

    private fun watchRecursively(dir: File, depth: Int) {
        if (depth > maxDepth) return
        if (!dir.exists() || !dir.isDirectory) return

        val observer = createObserverFor(dir, depth)
        observer.startWatching()
        observers.add(observer)

        try {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    watchRecursively(child, depth + 1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Papkani o'qib bo'lmadi: ${dir.path}", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun createObserverFor(dir: File, depth: Int): FileObserver {
        return object : FileObserver(dir.path, watchMask) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                val fullFile = File(dir, path)

                // Yangi papka yaratilsa — uni ham kuzatuv ostiga olish
                if ((event and CREATE) != 0 && fullFile.isDirectory) {
                    watchRecursively(fullFile, depth + 1)
                    return
                }

                if (fullFile.isFile) {
                    onFileEvent(fullFile)
                }
            }
        }
    }

    companion object {
        private const val TAG = "RecursiveFileObserver"
    }
}
