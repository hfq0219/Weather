package com.fengqi.weather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class Main_Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main_Activity.this);
        if (prefs.getString("weather", null) != null) {
            Intent intent = new Intent(Main_Activity.this, WeatherActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
