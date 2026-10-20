package com.saathi.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saathi.app.data.model.Medicine

@Dao
interface MedicineDao {

    /**
     * Full-text search across brand name and generic salt.
     * FTS4 virtual table enables fast substring matching on 100-row dataset.
     */
    @Query("""
        SELECT m.* FROM medicines m
        WHERE LOWER(m.brandName) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(m.genericSalt) LIKE '%' || LOWER(:query) || '%'
        LIMIT 10
    """)
    suspend fun searchMedicines(query: String): List<Medicine>

    /**
     * Look up by exact brand name match (used after OCR extracts text from strip).
     */
    @Query("SELECT * FROM medicines WHERE LOWER(brandName) LIKE '%' || LOWER(:brandName) || '%' LIMIT 5")
    suspend fun findByBrandName(brandName: String): List<Medicine>

    /**
     * Core of the Duplicate Pill Alert feature.
     * Given a list of already-scanned salts (the session's active medication list),
     * check whether a newly scanned salt overlaps.
     *
     * EXAMPLE: User scans "Glycomet 500" → salt = "Metformin Hydrochloride"
     *          Then scans "Glucophage 500" → salt = "Metformin Hydrochloride"
     *          This query finds the conflict and triggers the RED duplicate alert.
     */
    @Query("""
        SELECT * FROM medicines
        WHERE LOWER(genericSalt) IN (:scannedSalts)
        AND LOWER(genericSalt) = LOWER(:newSalt)
        LIMIT 1
    """)
    suspend fun findDuplicateSalt(newSalt: String, scannedSalts: List<String>): Medicine?

    @Query("SELECT * FROM medicines WHERE category = :category ORDER BY brandName")
    suspend fun getMedicinesByCategory(category: String): List<Medicine>

    @Query("SELECT * FROM medicines ORDER BY brandName")
    suspend fun getAllMedicines(): List<Medicine>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(medicines: List<Medicine>)
}
