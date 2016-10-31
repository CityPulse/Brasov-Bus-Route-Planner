package com.siemens.ct.citypulse.brasovbus;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

public class PreferencesActivity extends AppCompatActivity {

    private static final String TAG = "BrasovBus_Preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        CheckBox fastestCheckBox = (CheckBox) findViewById(R.id.fastestCheckBox);

        SharedPreferences app_preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        fastestCheckBox.setChecked(app_preferences.getBoolean(Constants.FASTEST, Constants.FASTEST_INITIAL_VALUE));

        fastestCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox fastestCheckBox = (CheckBox) view;
                SharedPreferences app_preferences =
                        PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putBoolean(Constants.FASTEST,fastestCheckBox.isChecked());
                editor.commit();
            }
        });

        CheckBox withMinimumBusStopscheckBox = (CheckBox) findViewById(R.id.withMinimumBusStopscheckBox);

        withMinimumBusStopscheckBox.setChecked(app_preferences.getBoolean(Constants.MIN_BUS_STOPS, Constants.MIN_BUS_STOPS_INITIAL_VALUE));

        withMinimumBusStopscheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox withMinimumBusStopscheckBox = (CheckBox) view;
                SharedPreferences app_preferences =
                        PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putBoolean(Constants.MIN_BUS_STOPS,withMinimumBusStopscheckBox.isChecked());
                editor.commit();
            }
        });

        CheckBox redBuscheckBox = (CheckBox) findViewById(R.id.redBuscheckBox);

        redBuscheckBox.setChecked(app_preferences.getBoolean(Constants.BUS_WITH_DISABILITY, Constants.BUS_WITH_DISABILITY_INITIAL_VALUE));

        redBuscheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox redBuscheckBox = (CheckBox) view;
                SharedPreferences app_preferences =
                        PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                SharedPreferences.Editor editor = app_preferences.edit();
                editor.putBoolean(Constants.BUS_WITH_DISABILITY,redBuscheckBox.isChecked());
                editor.commit();
            }
        });
    }
}
