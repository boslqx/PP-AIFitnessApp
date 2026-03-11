package com.example.aifitnessapp.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {

    private static AppExecutors instance;

    private final Executor diskIO;

    private AppExecutors() {
        diskIO = Executors.newSingleThreadExecutor();
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            instance = new AppExecutors();
        }
        return instance;
    }

    public Executor diskIO() {
        return diskIO;
    }
}