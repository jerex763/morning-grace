package com.morninggrace.bible.audio

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.morninggrace.bible.plan.SequentialPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

data class AudioChapter(val book: Int, val chapter: Int)

data class AudioLibraryStats(
    val chapterCount: Int,
    val totalBytes: Long
)

data class AudioImportResult(
    val imported: Int,
    val alreadyPresent: Int,
    val ignored: Int,
    val failedArchives: Int
)

data class AudioImportProgress(
    val archiveNumber: Int,
    val archiveCount: Int,
    val processedChapters: Int
)

class InsufficientStorageException(message: String) : IOException(message)

/**
 * Maps WordProject chapter filenames such as GEN_001_ck7_ef10.mp3 to canonical
 * Protestant Bible book/chapter numbers.
 */
object WordProjectAudioNaming {
    private val bookByCode = listOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
        "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO",
        "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO",
        "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL", "MAT",
        "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP",
        "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE",
        "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
    ).withIndex().associate { (index, code) -> code to index + 1 }

    private val wordProjectPattern =
        Regex("""(?:^|/)([1-3]?[A-Z]{2,3})_(\d{3})(?:_[^/]*)?\.mp3$""", RegexOption.IGNORE_CASE)
    private val canonicalPattern =
        Regex("""(?:^|/)(\d{2})_(\d{3})\.mp3$""", RegexOption.IGNORE_CASE)
    private val numberedFolderPattern =
        Regex("""(?:^|/)(\d{1,2})/(\d{1,3})\.mp3$""", RegexOption.IGNORE_CASE)

    fun parse(path: String): AudioChapter? {
        wordProjectPattern.find(path)?.let { match ->
            val book = bookByCode[match.groupValues[1].uppercase()] ?: return null
            val chapter = match.groupValues[2].toIntOrNull() ?: return null
            return validate(book, chapter)
        }
        canonicalPattern.find(path)?.let { match ->
            val book = match.groupValues[1].toIntOrNull() ?: return null
            val chapter = match.groupValues[2].toIntOrNull() ?: return null
            return validate(book, chapter)
        }
        numberedFolderPattern.find(path)?.let { match ->
            val book = match.groupValues[1].toIntOrNull() ?: return null
            val chapter = match.groupValues[2].toIntOrNull() ?: return null
            return validate(book, chapter)
        }
        return null
    }

    private fun validate(book: Int, chapter: Int): AudioChapter? {
        if (book !in 1..SequentialPlan.BOOK_CHAPTER_COUNTS.size) return null
        if (chapter !in 1..SequentialPlan.BOOK_CHAPTER_COUNTS[book - 1]) return null
        return AudioChapter(book, chapter)
    }
}

@Singleton
class BibleAudioLibrary @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val importMutex = Mutex()
    private val audioRoot: File
        get() {
            val musicRoot = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "music")
            return File(musicRoot, DIRECTORY_NAME).apply { mkdirs() }
        }

    fun chapterFile(book: Int, chapter: Int): File =
        File(audioRoot, canonicalFileName(book, chapter))

    fun findChapter(book: Int, chapter: Int): File? =
        chapterFile(book, chapter).takeIf(::isUsableMp3)

    fun stats(): AudioLibraryStats {
        val files = audioRoot.listFiles().orEmpty()
            .filter { WordProjectAudioNaming.parse(it.name) != null && isUsableMp3(it) }
        return AudioLibraryStats(
            chapterCount = files.size,
            totalBytes = files.sumOf { it.length() }
        )
    }

    suspend fun importZipArchives(
        resolver: ContentResolver,
        uris: List<Uri>,
        onProgress: (AudioImportProgress) -> Unit = {}
    ): AudioImportResult = withContext(Dispatchers.IO) {
        importMutex.withLock {
            ensureEnoughStorage(resolver, uris)
            importLocked(resolver, uris, onProgress)
        }
    }

    private suspend fun importLocked(
        resolver: ContentResolver,
        uris: List<Uri>,
        onProgress: (AudioImportProgress) -> Unit
    ): AudioImportResult {
        var imported = 0
        var alreadyPresent = 0
        var ignored = 0
        var failedArchives = 0
        var processedChapters = 0

        for ((archiveIndex, uri) in uris.withIndex()) {
            currentCoroutineContext().ensureActive()
            try {
                val input = resolver.openInputStream(uri)
                    ?: throw IOException("Cannot open $uri")
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        currentCoroutineContext().ensureActive()
                        if (!entry.isDirectory && entry.name.endsWith(".mp3", ignoreCase = true)) {
                            val chapter = WordProjectAudioNaming.parse(entry.name)
                            if (chapter == null) {
                                ignored++
                            } else {
                                val target = chapterFile(chapter.book, chapter.chapter)
                                if (isUsableMp3(target)) {
                                    alreadyPresent++
                                } else {
                                    target.delete()
                                    writeEntry(zip, target)
                                    imported++
                                }
                                processedChapters++
                                onProgress(
                                    AudioImportProgress(
                                        archiveNumber = archiveIndex + 1,
                                        archiveCount = uris.size,
                                        processedChapters = processedChapters
                                    )
                                )
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failedArchives++
            }
        }

        return AudioImportResult(imported, alreadyPresent, ignored, failedArchives)
    }

    private suspend fun writeEntry(zip: ZipInputStream, target: File) {
        val temporary = File(audioRoot, "${target.name}.part")
        temporary.delete()
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = zip.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_CHAPTER_BYTES) {
                        throw IOException("Audio chapter exceeds size limit")
                    }
                    output.write(buffer, 0, read)
                }
            }
            if (temporary.length() < MIN_CHAPTER_BYTES) {
                throw IOException("Audio chapter is empty or truncated")
            }
            if (!isUsableMp3(temporary)) {
                throw IOException("Audio chapter is not a valid MP3")
            }
            moveAtomically(temporary, target)
        } catch (error: Exception) {
            temporary.delete()
            if (!isUsableMp3(target)) target.delete()
            throw error
        }
    }

    private fun ensureEnoughStorage(resolver: ContentResolver, uris: List<Uri>) {
        val compressedBytes = uris.sumOf { uri ->
            runCatching {
                resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    descriptor.length.takeIf { it > 0L } ?: 0L
                } ?: 0L
            }.getOrDefault(0L)
        }
        if (compressedBytes <= 0L) return
        val required = (compressedBytes * STORAGE_OVERHEAD_FACTOR).toLong() + FREE_SPACE_RESERVE
        val available = StatFs(audioRoot.absolutePath).availableBytes
        if (available < required) {
            throw InsufficientStorageException(
                "录音包约需 ${required / (1024 * 1024)}MB，当前可用空间不足"
            )
        }
    }

    private fun moveAtomically(source: File, target: File) {
        target.parentFile?.mkdirs()
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {
            target.delete()
            if (!source.renameTo(target)) {
                throw IOException("Unable to finalize imported audio")
            }
        }
    }

    private fun isUsableMp3(file: File): Boolean {
        if (!file.isFile || file.length() < MIN_CHAPTER_BYTES) return false
        return runCatching {
            RandomAccessFile(file, "r").use { input ->
                val first = input.readUnsignedByte()
                val second = input.readUnsignedByte()
                (first == 'I'.code && second == 'D'.code) ||
                    (first == 0xFF && second and 0xE0 == 0xE0)
            }
        }.getOrDefault(false)
    }

    companion object {
        const val TOTAL_BIBLE_CHAPTERS = 1189
        const val DIRECTORY_NAME = "bible-audio"
        private const val MIN_CHAPTER_BYTES = 8 * 1024L
        private const val MAX_CHAPTER_BYTES = 64 * 1024 * 1024L
        private const val FREE_SPACE_RESERVE = 256 * 1024 * 1024L
        private const val STORAGE_OVERHEAD_FACTOR = 1.15

        fun canonicalFileName(book: Int, chapter: Int): String =
            "%02d_%03d.mp3".format(book, chapter)
    }
}
