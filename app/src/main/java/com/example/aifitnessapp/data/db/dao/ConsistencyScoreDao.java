package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.ConsistencyScore;
import java.util.List;

@Dao
public interface ConsistencyScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConsistencyScore score);
    // REPLACE because we recompute and overwrite today's score each time

    @Query("SELECT * FROM consistency_score WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    LiveData<List<ConsistencyScore>> getLast7Scores(int userId);

    // Plain List for behavior pattern detector (background thread)
    @Query("SELECT * FROM consistency_score WHERE userId = :userId ORDER BY date DESC LIMIT 14")
    List<ConsistencyScore> getLast14ScoresSync(int userId);

    @Query("SELECT * FROM consistency_score WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    List<ConsistencyScore> getLast7ScoresSync(int userId);

    @Query("SELECT AVG(score) FROM consistency_score WHERE userId = :userId AND date >= :fromDate")
    float getAvgScoreSince(int userId, String fromDate);
}