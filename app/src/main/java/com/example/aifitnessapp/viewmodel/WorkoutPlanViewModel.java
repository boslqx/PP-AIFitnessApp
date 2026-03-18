package com.example.aifitnessapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.model.WorkoutPlan;
import com.example.aifitnessapp.repository.WorkoutPlanRepository;
import com.example.aifitnessapp.util.AppExecutors;

public class WorkoutPlanViewModel extends AndroidViewModel {

    private WorkoutPlanRepository repository;
    public LiveData<UserProfile>      currentUser;
    public MutableLiveData<WorkoutPlan> workoutPlan = new MutableLiveData<>();
    public MutableLiveData<Boolean>     isLoading   = new MutableLiveData<>(true);

    public WorkoutPlanViewModel(@NonNull Application application) {
        super(application);
        repository  = new WorkoutPlanRepository(application);
        currentUser = FitAIDatabase.getInstance(application)
                .userProfileDao().getCurrentUser();
    }

    public void generatePlan(int userId) {
        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            WorkoutPlan plan = repository.generatePlanForUser(userId);
            workoutPlan.postValue(plan);
            isLoading.postValue(false);
        });
    }
}