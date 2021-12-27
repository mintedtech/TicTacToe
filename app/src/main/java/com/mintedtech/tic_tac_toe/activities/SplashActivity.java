package com.mintedtech.tic_tac_toe.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.mintedtech.tic_tac_toe.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class SplashActivity extends AppCompatActivity
{
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        PreferenceManager.setDefaultValues (getApplicationContext (), R.xml.root_preferences, true);

        AppCompatDelegate.setDefaultNightMode (Build.VERSION.SDK_INT < 28
                                               ? AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                                               : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        startActivity (new Intent (getApplicationContext (), MainActivity.class));
        finish ();
    }
}
