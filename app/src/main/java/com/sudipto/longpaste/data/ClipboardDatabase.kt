package com.sudipto.longpaste.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ClipboardItem(
    val id: Long,
    val title: String?,
    val content: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val isPinned: Boolean
)

class ClipboardDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT,
                $COL_CONTENT TEXT NOT NULL,
                $COL_CREATED INTEGER NOT NULL,
                $COL_USED INTEGER NOT NULL,
                $COL_PINNED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clipboard_used ON $TABLE($COL_USED DESC)")
        db.execSQL("CREATE INDEX idx_clipboard_pinned ON $TABLE($COL_PINNED DESC, $COL_USED DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Reserved for future schema migrations.
    }

    fun insertOrTouch(content: String, title: String? = null): Long {
        val now = System.currentTimeMillis()
        writableDatabase.query(
            TABLE,
            arrayOf(COL_ID),
            "$COL_CONTENT = ?",
            arrayOf(content),
            null, null, null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                writableDatabase.update(
                    TABLE,
                    ContentValues().apply {
                        put(COL_USED, now)
                        if (title != null) put(COL_TITLE, title)
                    },
                    "$COL_ID = ?",
                    arrayOf(id.toString())
                )
                return id
            }
        }

        return writableDatabase.insertOrThrow(
            TABLE,
            null,
            ContentValues().apply {
                put(COL_TITLE, title)
                put(COL_CONTENT, content)
                put(COL_CREATED, now)
                put(COL_USED, now)
                put(COL_PINNED, 0)
            }
        )
    }

    fun list(limit: Int = 100, query: String = ""): List<ClipboardItem> = buildList {
        val selection = if (query.isBlank()) null else "($COL_TITLE LIKE ? OR $COL_CONTENT LIKE ?)"
        val args = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        readableDatabase.query(
            TABLE,
            arrayOf(
                COL_ID,
                COL_TITLE,
                "substr($COL_CONTENT, 1, 512) AS preview",
                "$COL_CREATED AS created",
                "$COL_USED AS used",
                "$COL_PINNED AS pinned"
            ),
            selection,
            args,
            null,
            null,
            "$COL_PINNED DESC, $COL_USED DESC",
            limit.toString()
        ).use { cursor ->
            val idIx = cursor.getColumnIndexOrThrow(COL_ID)
            val titleIx = cursor.getColumnIndexOrThrow(COL_TITLE)
            val previewIx = cursor.getColumnIndexOrThrow("preview")
            val createdIx = cursor.getColumnIndexOrThrow("created")
            val usedIx = cursor.getColumnIndexOrThrow("used")
            val pinnedIx = cursor.getColumnIndexOrThrow("pinned")
            while (cursor.moveToNext()) {
                add(
                    ClipboardItem(
                        id = cursor.getLong(idIx),
                        title = cursor.getString(titleIx),
                        content = cursor.getString(previewIx) ?: "",
                        createdAt = cursor.getLong(createdIx),
                        lastUsedAt = cursor.getLong(usedIx),
                        isPinned = cursor.getInt(pinnedIx) != 0
                    )
                )
            }
        }
    }

    fun totalCharacters(id: Long): Long? = readableDatabase.rawQuery(
        "SELECT length($COL_CONTENT) FROM $TABLE WHERE $COL_ID = ?",
        arrayOf(id.toString())
    ).use { cursor ->
        if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getLong(0)
    }

    fun totalUtf8Bytes(id: Long): Long? = readableDatabase.rawQuery(
        "SELECT length(CAST($COL_CONTENT AS BLOB)) FROM $TABLE WHERE $COL_ID = ?",
        arrayOf(id.toString())
    ).use { cursor ->
        if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getLong(0)
    }

    fun get(id: Long): ClipboardItem? = readableDatabase.query(
        TABLE,
        null,
        "$COL_ID = ?",
        arrayOf(id.toString()),
        null, null, null, "1"
    ).use { cursor ->
        if (!cursor.moveToFirst()) return null
        ClipboardItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED)),
            lastUsedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USED)),
            isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PINNED)) != 0
        )
    }

    fun setPinned(id: Long, pinned: Boolean) {
        writableDatabase.update(
            TABLE,
            ContentValues().apply { put(COL_PINNED, if (pinned) 1 else 0) },
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete(TABLE, null, null)
    }

    companion object {
        private const val DB_NAME = "longpaste.db"
        private const val DB_VERSION = 1
        private const val TABLE = "clipboard_items"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_CREATED = "created_at"
        private const val COL_USED = "last_used_at"
        private const val COL_PINNED = "is_pinned"
    }
}
