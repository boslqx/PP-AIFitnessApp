package com.example.aifitnessapp.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.aifitnessapp.data.model.UserProfile;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);
    // REPLACE means: if a row with the same primary key exists, overwrite it
    // This doubles as an "upsert" (insert or update)

    @Update
    void update(UserProfile profile);

    @Delete
    void delete(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    LiveData<UserProfile> getUserById(int userId);

    @Query("SELECT * FROM user_profile LIMIT 1")
    LiveData<UserProfile> getCurrentUser();
    // LIMIT 1 because Phase 1 supports only one user profile
}
