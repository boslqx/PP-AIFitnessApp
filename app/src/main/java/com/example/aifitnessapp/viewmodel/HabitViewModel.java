package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.repository.HabitRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitViewModel extends AndroidViewModel {

    private HabitRepository repository;

    public LiveData<UserProfile>     currentUser;
    public LiveData<List<HabitLog>>  todayHabits;

    // Completion summary: "3 / 5 habits done"
    public MutableLiveData<String> completionSummary = new MutableLiveData<>("");

    public HabitViewModel(@NonNull Application application) {
        super(application);
        repository  = new HabitRepository(application);
        currentUser = FitAIDatabase.getInstance(application)
                .userProfileDao().getCurrentUser();
    }

    public void loadTodayHabits(int userId) {
        todayHabits = repository.getHabitsByDate(userId, getToday());
    }

    /*
     * Called whenever the habit list updates.
     * Counts completed vs total and posts a summary string.
     */
    public void refreshSummary(List<HabitLog> habits) {
        if (habits == null || habits.isEmpty()) {
            completionSummary.postValue("No habits yet — add your first one below!");
            return;
        }
        int done  = 0;
        for (HabitLog h : habits) if (h.completed) done++;
        int total = habits.size();

        String emoji = done == total ? " 🎉" : done == 0 ? "" : " 💪";
        completionSummary.postValue(done + " / " + total + " habits done today" + emoji);
    }

    /*
     * Creates a new HabitLog for today with completed=false.
     * The user will tap it to mark it done.
     */
    public void addHabit(int userId, String habitName, String category) {
        HabitLog habit    = new HabitLog();
        habit.userId      = userId;
        habit.date        = getToday();
        habit.habitName   = habitName;
        habit.category    = category;
        habit.completed   = false;
        habit.loggedAt    = System.currentTimeMillis();
        repository.insertHabit(habit);
    }

    /*
     * Toggles the completed state of a habit.
     * We flip the boolean and update — Room LiveData re-delivers
     * the updated list to the observer automatically.
     */
    public void toggleHabit(HabitLog habit) {
        habit.completed = !habit.completed;
        repository.updateHabit(habit);
    }

    public void deleteHabit(HabitLog habit) {
        repository.deleteHabit(habit);
    }

    public String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public String getTodayDisplay() {
        return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
    }
}