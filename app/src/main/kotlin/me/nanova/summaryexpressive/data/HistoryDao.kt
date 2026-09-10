package me.nanova.summaryexpressive.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.nanova.summaryexpressive.data.local.database.entity.HistoryEntity
import me.nanova.summaryexpressive.model.SummaryType

@Dao
interface HistoryDao {
    @Query(
        """
         SELECT * FROM history 
         WHERE (:type IS NULL OR type = :type) 
         AND (:query = '' OR title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR lengthResults LIKE '%' || :query || '%')
         ORDER BY createdOn DESC
    """
    )
    fun getSummaries(query: String, type: SummaryType?): PagingSource<Int, HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: HistoryEntity)

    @Query("SELECT * FROM history WHERE sourceLink = :link ORDER BY createdOn DESC")
    suspend fun getAllBySourceLink(link: String): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE sourceText = :text ORDER BY createdOn DESC")
    suspend fun getAllBySourceText(text: String): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY createdOn DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}