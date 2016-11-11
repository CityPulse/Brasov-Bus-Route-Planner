package com.siemens.ct.citypulse.brasovbus;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import citypulse.commons.contextual_filtering.city_event_ontology.CityEvent;
import citypulse.commons.contextual_filtering.city_event_ontology.CriticalEventResults;
import citypulse.commons.contextual_filtering.contextual_event_request.Place;
import citypulse.commons.contextual_filtering.contextual_event_request.PlaceAdapter;

public class NavigationActivity extends AppCompatActivity {

    private Location gpsLocation = null;
    private static final String TAG = "BrasovBus_Navigation";
    private ListView resultsListView;
    private int selectedItemNumber = -1;

    private List<AlertDialog> allerDialogList = new ArrayList();

    private BroadcastReceiver alertsBroadcastReceiver;
    private BroadcastReceiver locationBroadcastReceiver;
    private BroadcastReceiver errorBroadcastReceiver;

    private LinkedList<String> listOfStations = new LinkedList<String>();
    private String closestBusStationName = null;

    int previousPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        resultsListView = (ListView) findViewById(R.id.navigareListView);
        resultsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        resultsListView.setClickable(false);
        resultsListView.setLongClickable(false);

        CompleteBusStationDetails completeBusStationDetails = new CompleteBusStationDetails();

        //get the extra
        Bundle extras = getIntent().getExtras();

        String selectedRoute = extras.getString("selectedRoute");

        Log.i(TAG,"selectedRoute: "+selectedRoute);

        if (extras == null) {
            Log.i(TAG,"ERROR: no extras in intent request");
        } else {
            Type type = new TypeToken<CompleteBusStationDetails>() {
            }.getType();
            completeBusStationDetails = new Gson().fromJson(extras.getString("selectedRoute"), type);
        }
        //end get extra

        for(String busNumber : completeBusStationDetails.getBusNumberAndStationsLinker().keySet())
        {
            for(BusStation busStation : completeBusStationDetails.getBusNumberAndStationsLinker().get(busNumber)) {
                if(!listOfStations.isEmpty() && listOfStations.get(listOfStations.size()-1).equals(busStation.getBusStationName()))
                    continue;
                listOfStations.add(busStation.getBusStationName());
            }
        }

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedItemNumber = i;
            }
        });

        ArrayAdapter simpleAdapter = new ArrayAdapter (this,
                android.R.layout.simple_list_item_1, listOfStations);
        resultsListView.setAdapter(simpleAdapter);

        Log.i(TAG, "Starting navigation");

        //interpret the received command
        if(completeBusStationDetails!=null) {
            Intent eventNotificationServiceIntent = new Intent(this, EventNotificationService.class);
            eventNotificationServiceIntent.putExtra("cf_request", new Gson().toJson(completeBusStationDetails));
            startService(eventNotificationServiceIntent);
        }

        //========== Starting receivers ==============
        startLocationUpdateBroadcastReceiver();
        startAlertsBroadcastReceiver();
        startErrorBroadcastReceiver();
        //============================================

        Button reportTrafficIncidentButton = (Button) findViewById(R.id.reportTrafficIncidentButton);

        reportTrafficIncidentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i(TAG,"Report traffic incidents button was pressed !");

                if(closestBusStationName!=null) {
                    Intent reportIncidentActivityIntent = new Intent(NavigationActivity.this, ReportTrafficIncidentActivity.class);
                    System.out.println("Constants.busStationCoordinates.get(Constants.busStations.indexOf(closestBusStationName)): " + Constants.busStationCoordinates.get(Constants.busStations.indexOf(closestBusStationName)));
                    reportIncidentActivityIntent.putExtra("closestBusStationCoordinates", new Gson().toJson(Constants.busStationCoordinates.get(Constants.busStations.indexOf(closestBusStationName)))); //Optional parameters
                    NavigationActivity.this.startActivity(reportIncidentActivityIntent);
                }
                else
                {
                    Intent errorActivityIntent = new Intent(NavigationActivity.this, ErrorReportActivity.class);
                    errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "Unknown location. Waiting for GSP to establish correct location.");
                    NavigationActivity.this.startActivity(errorActivityIntent);

                    Toast.makeText(NavigationActivity.this,
                            "Unknown location. Waiting for GSP to establish correct location.",
                            Toast.LENGTH_LONG).show();
                    Log.i(TAG,"WARNING: Unknown location. Waiting for GSP to establish correct location.");
                }
            }
        });
    }

    private void startLocationUpdateBroadcastReceiver() {

        Log.i(TAG, "Started LOCATION receiver");

        locationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                closestBusStationName = intent
                        .getStringExtra(Constants.LOCATION_UPDATE_MESSAGE_PAYLOAD);

                if(listOfStations.contains(closestBusStationName)) {

                    Log.i(TAG, "Station: "+closestBusStationName+" at index: "+listOfStations.indexOf(closestBusStationName));
                    int position = listOfStations.indexOf(closestBusStationName);

                    if(previousPosition==-1)
                    {
                        for(int i=0;i<listOfStations.size();i++)
                            resultsListView.getChildAt(i).setBackgroundResource(R.color.colorDefault);

                        resultsListView.getChildAt(position).setBackgroundResource(R.color.colorPrimaryLight);
                        previousPosition = position;
                    }

                    if(position != previousPosition) {
                        resultsListView.getChildAt(previousPosition).setBackgroundResource(R.color.colorDefault);
                        resultsListView.getChildAt(position).setBackgroundResource(R.color.colorPrimaryLight);
                        previousPosition = position;
                    }

                    /*resultsListView.getChildAt(position).setBackgroundColor(0xff0000ff);
                    previousPosition = position;

                    for(int i=0; i<listOfStations.size();i++) {
                        if(i==position)
                            resultsListView.getChildAt(position).setBackgroundColor(0xff0000ff);
                        else
                            resultsListView.getChildAt(i).setBackgroundColor(0xffffffff);
                    }*/
                }
            }
        };

        IntentFilter latestLocationIntentFilter = new IntentFilter();
        latestLocationIntentFilter.addAction(Constants.LOCATION_UPDATE_MESSAGE);
        registerReceiver(locationBroadcastReceiver, latestLocationIntentFilter);

    }

    private void startAlertsBroadcastReceiver() {

        Log.i(TAG, "Started ALERT receiver");

        alertsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String criticalEventResultsString = intent
                        .getStringExtra(Constants.EVENT_ALERT_MESSAGE_PAYLOAD);

                Log.i(TAG,"Critical event was received: "
                        + criticalEventResultsString);

                GsonBuilder builder = new GsonBuilder();
                builder.registerTypeAdapter(Place.class, new PlaceAdapter());
                Gson gson = builder.create();

                CriticalEventResults criticalEventResults = gson.fromJson(
                        criticalEventResultsString, CriticalEventResults.class);

                StringBuilder messageStringBuilder = new StringBuilder(
                        "The following events have been received: ");

                Boolean eventOK = true;


                for (CityEvent contextualEvent : criticalEventResults
                        .getContextualEvents()) {

                    if (contextualEvent.getEventLevel() > 0) {
                        messageStringBuilder.append(contextualEvent
                                .getEventCategory()
                                + "[level = "
                               + contextualEvent.getEventLevel()
                                + ", coordinates("
                                + contextualEvent.getEventPlace()
                                .getCentreCoordinate().toString()
                                + ")]; ");
                    } else {
                       eventOK = false;
                    }
                }

               if (eventOK) {

                    //for used wne displaying pop-ups. It's role is to lose the opened one if another one comes
                    /*for (AlertDialog alertDialog : allerDialogList) {
                        if (alertDialog.isShowing())
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        alertDialog.cancel();
                    }*/

                    AlertDialog alertDialog = new AlertDialog.Builder(
                            NavigationActivity.this)
                            .setTitle("Event notification")
                            .setMessage(messageStringBuilder.toString())
                            .setNegativeButton("Go back to route selection",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                               int which) {
                                            /*Intent intent = new Intent();
                                            //intent.setAction(Constants.COMMAND_GO_TO_TRAVEL_RECOMANDATION);
                                            NavigationActivity.this
                                                    .sendBroadcast(intent);
                                            NavigationActivity.this.finish();*/

                                            finish();
                                        }
                                    })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setNeutralButton("Continue",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            Log.i(TAG, "The user has decided to continue.");
                                        }
                                    }
                            )
                    .show();

                    allerDialogList.add(alertDialog);
                } else {
                   Log.i(TAG,"The event was not displayed because it was not well formated!");
                }
            }
        };

        IntentFilter alertIntentFilter = new IntentFilter();
        alertIntentFilter.addAction(Constants.EVENT_ALERT_MESSAGE);
        registerReceiver(alertsBroadcastReceiver, alertIntentFilter);
    }

    private void startErrorBroadcastReceiver() {

        Log.i(TAG, "Started ERROR receiver");

        errorBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String errorMessage = intent
                        .getStringExtra(Constants.ERROR_MESSAGE_PAYLOAD);

                Intent intentError = new Intent(NavigationActivity.this,
                        ErrorReportActivity.class);

                intentError.putExtra("Error", errorMessage);
                startActivity(intentError);

            }
        };

        IntentFilter errorIntentFilter = new IntentFilter();
        errorIntentFilter.addAction(Constants.ERROR_MESSAGE);
        registerReceiver(errorBroadcastReceiver, errorIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "Dismissing all dialogs in NavigationActivity");
        for(AlertDialog i : allerDialogList) {
            i.dismiss();
        }

        unRegisterReceivers();
        Log.i(TAG, "Stopping service EventNotificationService");
        stopService(new Intent(NavigationActivity.this,EventNotificationService.class));
        finish();
    }

    private void unRegisterReceivers() {
        if(locationBroadcastReceiver!=null) {
            Log.i(TAG, "Unregistered locationBroadcastReceiver");
            unregisterReceiver(locationBroadcastReceiver);
        }
        if(alertsBroadcastReceiver!=null) {
            Log.i(TAG, "Unregistered alertsBroadcastReceiver");
            unregisterReceiver(alertsBroadcastReceiver);
        }
        if(errorBroadcastReceiver!=null) {
            Log.i(TAG, "Unregistered errorBroadcastReceiver");
            unregisterReceiver(errorBroadcastReceiver);
        }
    }
}
