package com.siemens.ct.citypulse.brasovbus;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.siemens.citypulse.busMessages.incidents.Incident;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;

import citypulse.commons.data.Coordinate;


public class ReportTrafficIncidentActivity extends AppCompatActivity implements LocationListener {

    private Location location = null;

    private static final String TAG = "BrasovBus_ReportTraffic";
    private Incident incident = null;
    private LocationManager locationManager;

    private BroadcastReceiver locationBroadcastReceiver;
    private String closestBusStationName = null;
    private Coordinate closestBusStationCoordinatesInitial = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_traffic_incident);

        Bundle extras = getIntent().getExtras();

        String closestBusStationCoordinatesInitialString = extras.getString("closestBusStationCoordinates");

        if (extras == null) {
            Log.i(TAG,"ERROR: no extras in intent request");
        } else {
            Type type = new TypeToken<Coordinate>() {
            }.getType();
            closestBusStationCoordinatesInitial = new Gson().fromJson(closestBusStationCoordinatesInitialString, type);
        }

        Log.i(TAG, "closestBusStationCoordinatesInitialString: "+closestBusStationCoordinatesInitialString);

        startLocationUpdateBroadcastReceiver();

        Button okReportIncidentButton = (Button) findViewById(R.id.okReportIncidentButton);

        okReportIncidentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                incident = new Incident();

                RadioButton trafficCongestionRadioButton = (RadioButton)findViewById(R.id.trafficCongestionRadioButton);
                RadioButton roadWorksRadioButton = (RadioButton)findViewById(R.id.roadWorksRadioButton);

                if(trafficCongestionRadioButton.isChecked()){
                    incident.setCategory("Traffic congestion");
                }

                if(roadWorksRadioButton.isChecked()){
                    incident.setCategory("Road works");
                }

                //System.out.println(System.currentTimeMillis());
                String timestamp = new String(Long.toString(System.currentTimeMillis()));
                timestamp = timestamp.substring(0, 10); //limit the timestamp length to 10 numbers
                incident.setTimestamp(timestamp);

                if(location == null){
                    Intent intent = new Intent();
                    intent.setAction(Constants.ERROR_MESSAGE);
                    intent.putExtra(Constants.ERROR_MESSAGE_PAYLOAD,
                            "Unable to determine current location. Please try again later.");
                    sendBroadcast(intent);

                    Log.i(TAG,"ERROR: unable to send the request because because there is no gps coverage.");
                    Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
                    errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "ERROR: unable to send the " +
                            "request because because there is no gps coverage. Please make sure that" +
                            " the gps module is turned on and wait until the system determines the location.");
                    ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
                    return;

                }

                if(closestBusStationName == null)
                {
                    Coordinate closestBusStationCoordinates = closestBusStationCoordinatesInitial;

                    incident.setLatitude(closestBusStationCoordinates.getLatitude());
                    incident.setLongitude(closestBusStationCoordinates.getLongitude());

                    TrafficIncidentReportConnector trafficIncidentReportConnector = new TrafficIncidentReportConnector();
                    trafficIncidentReportConnector.execute();

                    Toast.makeText(ReportTrafficIncidentActivity.this,
                            "Please wait until the application registers the incident (init).",
                            Toast.LENGTH_SHORT).show();

                }
                else
                {
                    Coordinate closestBusStationCoordinates = Constants.busStationCoordinates.get(Constants.busStations.indexOf(closestBusStationName));

                    incident.setLatitude(closestBusStationCoordinates.getLatitude());
                    incident.setLongitude(closestBusStationCoordinates.getLongitude());

                    TrafficIncidentReportConnector trafficIncidentReportConnector = new TrafficIncidentReportConnector();
                    trafficIncidentReportConnector.execute();

                    Toast.makeText(ReportTrafficIncidentActivity.this,
                            "Please wait until the application registers the incident.",
                            Toast.LENGTH_SHORT).show();

                }

            }
        });
    }

    private void startLocationUpdateBroadcastReceiver() {

        Log.i(TAG, "Started receiver from ReportTrafficIncidentActivity");

        locationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                closestBusStationName = intent.getStringExtra(Constants.LOCATION_UPDATE_MESSAGE_PAYLOAD);
                Log.i(TAG, "closestBusStationName in REPORT ACTIVITY: "+closestBusStationName);
            }
        };

        IntentFilter latestLocationIntentFilter = new IntentFilter();
        latestLocationIntentFilter.addAction(Constants.LOCATION_UPDATE_MESSAGE);
        registerReceiver(locationBroadcastReceiver, latestLocationIntentFilter);

    }

    private void logError(Exception e) {
        Log.i(TAG,"ERROR: unable to send the request because the following rest service is not available: " + Constants.TRAFFIC_INCIDENTS_REPORT_END_POINT, e);
        Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
        errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "ERROR: unable to access " +
                "the rest service: " + Constants.TRAFFIC_INCIDENTS_REPORT_END_POINT
                + " Please check if the device is connected to the internet.");
        ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
            errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");
            ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
            Log.i(TAG,"The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, ReportTrafficIncidentActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
            errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");
            ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
            Log.i(TAG,"The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");

            return;
        }
        locationManager.removeUpdates(ReportTrafficIncidentActivity.this);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
            errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");
            ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
            Log.i(TAG,"The application does not have the " +
                    "permission to use the GPS location. The app cannot sent the request.");

            return;
        }
        locationManager.removeUpdates(ReportTrafficIncidentActivity.this);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterReceivers();

    }

    private void unRegisterReceivers() {
        if(locationBroadcastReceiver!=null) {
            unregisterReceiver(locationBroadcastReceiver);
        }
    }

    class TrafficIncidentReportConnector extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {

            URLConnection connection = null;

            try {
                connection = new URL(Constants.TRAFFIC_INCIDENTS_REPORT_END_POINT).openConnection();
            } catch (IOException e) {

                logError(e);
                return null;

            }

            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(Constants.TRAFFIC_INCIDENTS_REPORT_TIMEOUT);

            OutputStreamWriter output = null;

            try {
                output = new OutputStreamWriter(connection.getOutputStream());
            } catch (IOException e) {
                logError(e);
                return null;
            }

            try {
                Log.i(TAG, "incident: "+new Gson().toJson(incident));
                output.write(String.valueOf((new Gson().toJson(incident))));
                output.close();
            } catch (IOException e) {
                logError(e);
                return null;
            }

            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
            } catch (IOException e) {
                logError(e);
                return null;
            }

            String responseValue = null;
            try {
                responseValue = br.readLine();
            } catch (IOException e) {
                logError(e);
                return null;
            }
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(responseValue.equals("OK")){
                ReportTrafficIncidentActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReportTrafficIncidentActivity.this,
                                "The incident was registered successfully.",
                                Toast.LENGTH_LONG).show();
                    }
                });
                Log.i(TAG,"INFO: incident: "+incident+", has been successfully registered");
/*                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                //finish();
            }else{
                Log.i(TAG,"ERROR: unable to send the request because the following rest service " +
                        "is not available: " + Constants.TRAFFIC_INCIDENTS_REPORT_END_POINT +
                        " or there is a communication error");
                Intent errorActivityIntent = new Intent(ReportTrafficIncidentActivity.this, ErrorReportActivity.class);
                errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "ERROR: unable to access " +
                        "the rest service: " + Constants.TRAFFIC_INCIDENTS_REPORT_END_POINT +
                        " or there is a communication error. Please check if the device is " +
                        "connected to the internet.");
                ReportTrafficIncidentActivity.this.startActivity(errorActivityIntent);
            }

            return null;
        }
    }
}
