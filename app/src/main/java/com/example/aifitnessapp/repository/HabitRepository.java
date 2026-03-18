package com.example.aifitnessapp.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.util.AppExecutors;
import java.util.List;

public class HabitRepository {

    private HabitLogDao habitLogDao;

    public HabitRepository(Application application) {
        habitLogDao = FitAIDatabase.getInstance(application).habitLogDao();
    }

    /*
     * LiveData of today's habits — Room re-delivers this automatically
     * whenever a habit is inserted or updated, so the UI stays in sync
     * without any manual refresh calls.
     */
    public LiveData<List<HabitLog>> getHabitsByDate(int userId, String date) {
        return habitLogDao.getHabitsByDate(userId, date);
    }

    public LiveData<List<HabitLog>> getLast7DaysHabits(int userId) {
        return habitLogDao.getLast7DaysHabits(userId);
    }

    // Insert a new habit on background thread
    public void insertHabit(HabitLog habit) {
        AppExecutors.getInstance().diskIO().execute(() ->
                habitLogDao.insert(habit));
    }

    // Update (e.g. toggle completed) on background thread
    public void updateHabit(HabitLog habit) {
        AppExecutors.getInstance().diskIO().execute(() ->
                habitLogDao.update(habit));
    }

    public void deleteHabit(HabitLog habit) {
        AppExecutors.getInstance().diskIO().execute(() ->
                habitLogDao.delete(habit));
    }
}