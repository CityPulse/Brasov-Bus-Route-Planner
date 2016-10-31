package com.siemens.ct.citypulse.brasovbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ErrorReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_report);

        Intent intent = getIntent();

        String error = intent.getStringExtra(Constants.ERROR_MESSAGE);

        TextView errorTextView = (TextView) findViewById(R.id.errorTextView);

        System.out.println("error: "+error.toCharArray());
        errorTextView.setText(error.toCharArray(),0,error.length());
    }
}
