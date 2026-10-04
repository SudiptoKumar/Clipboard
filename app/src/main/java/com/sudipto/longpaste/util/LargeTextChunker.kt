package com.sudipto.longpaste.util

object LargeTextChunker {
    const val DEFAULT_CHUNK_SIZE = 16_384

    /**
     * Splits text into UTF-16-safe chunks for repeated InputConnection.commitText calls.
     * A surrogate pair is never split across two chunks.
     */
    fun chunks(text: CharSequence, chunkSize: Int = DEFAULT_CHUNK_SIZE): Sequence<CharSequence> {
        require(chunkSize > 0) { "chunkSize must be positive" }
        return sequence {
            var start = 0
            while (start < text.length) {
                var end = minOf(start + chunkSize, text.length)
                if (end < text.length && end > start && Character.isHighSurrogate(text[end - 1])) {
                    end -= 1
                }
                if (end == start) {
                    end = minOf(start + 2, text.length)
                }
                yield(text.subSequence(start, end))
                start = end
            }
        }
    }

    fun count(text: CharSequence, chunkSize: Int = DEFAULT_CHUNK_SIZE): Int {
        require(chunkSize > 0) { "chunkSize must be positive" }
        return chunks(text, chunkSize).count()
    }
}
