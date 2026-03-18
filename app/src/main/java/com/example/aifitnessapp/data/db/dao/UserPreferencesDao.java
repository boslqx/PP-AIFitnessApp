package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.UserPreferences;

@Dao
public interface UserPreferencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserPreferences prefs);

    @Update
    void update(UserPreferences prefs);

    @Query("SELECT * FROM user_preferences ORDER BY id ASC LIMIT 1")
    LiveData<UserPreferences> getCurrentUser();

    @Query("SELECT * FROM user_preferences ORDER BY id ASC LIMIT 1")
    UserPreferences getCurrentUserSync();

    @Query("SELECT COUNT(*) FROM user_preferences")
    int getUserCount();
}