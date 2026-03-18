package com.example.aifitnessapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Router — will be wired up once new screens are built
        setContentView(R.layout.activity_main);
    }
}