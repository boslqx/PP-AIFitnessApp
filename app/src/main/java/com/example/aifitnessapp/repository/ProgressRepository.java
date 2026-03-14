package com.example.aifitnessapp.repository;

import android.app.Application;
import com.example.aifitnessapp.data.db.FitAIDatabase;
import com.example.aifitnessapp.data.db.dao.DailyLogDao;
import com.example.aifitnessapp.data.model.DailyLog;
import java.util.List;

public class ProgressRepository {

    private DailyLogDao dailyLogDao;

    public ProgressRepository(Application application) {
        dailyLogDao = FitAIDatabase.getInstance(application).dailyLogDao();
    }

    /*
     * Returns ALL logs for a user in ascending date order.
     * ProgressViewModel will slice these into separate chart datasets.
     * We use Sync (plain List) because this always runs on diskIO thread.
     */
    public List<DailyLog> getAllLogsSync(int userId) {
        return dailyLogDao.getAllLogsSync(userId);
    }
}