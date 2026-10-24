package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sevacare.app.data.model.ScamPhrase

@Dao
interface ScamPhraseDao {

    /**
     * Returns all scam phrases matching any words in the scanned text.
     * Used by the Ruko Mode detector to calculate a total scam score.
     */
    @Query("""
        SELECT * FROM scam_phrases
        WHERE LOWER(:scannedText) LIKE '%' || LOWER(phrase) || '%'
    """)
    suspend fun findMatchingPhrases(scannedText: String): List<ScamPhrase>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(phrases: List<ScamPhrase>)
}
