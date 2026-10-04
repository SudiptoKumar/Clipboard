package com.sudipto.longpaste.util

object LargeTextChunker {
    const val DEFAULT_CHUNK_SIZE = 16_384

    fun chunks(text: CharSequence, chunkSize: Int = DEFAULT_CHUNK_SIZE): Sequence<CharSequence> {
        require(chunkSize > 0) { "chunkSize must be positive" }
        return sequence {
            var start = 0
            while (start < text.length) {
                val end = minOf(start + chunkSize, text.length)
                yield(text.subSequence(start, end))
                start = end
            }
        }
    }

    fun count(text: CharSequence, chunkSize: Int = DEFAULT_CHUNK_SIZE): Int {
        require(chunkSize > 0) { "chunkSize must be positive" }
        if (text.isEmpty()) return 0
        return (text.length + chunkSize - 1) / chunkSize
    }
}
