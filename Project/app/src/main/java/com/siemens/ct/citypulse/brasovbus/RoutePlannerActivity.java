package com.siemens.ct.citypulse.brasovbus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Set;

import citypulse.commons.contextual_filtering.contextual_event_request.Route;
import citypulse.commons.data.Coordinate;
import citypulse.commons.reasoning_request.ARType;
import citypulse.commons.reasoning_request.Answer;
import citypulse.commons.reasoning_request.ReasoningRequest;
import citypulse.commons.reasoning_request.User;
import citypulse.commons.reasoning_request.concrete.AnswerAdapter;
import citypulse.commons.reasoning_request.concrete.FunctionalConstraintValueAdapter;
import citypulse.commons.reasoning_request.concrete.FunctionalParameterValueAdapter;
import citypulse.commons.reasoning_request.concrete.IntegerFunctionalConstraintValue;
import citypulse.commons.reasoning_request.concrete.StringFunctionalParameterValue;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalConstraint;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalConstraintName;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalConstraintOperator;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalConstraintValue;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalConstraints;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalDetails;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalParameter;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalParameterName;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalParameterValue;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalParameters;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalPreference;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalPreferenceOperation;
import citypulse.commons.reasoning_request.functional_requirements.FunctionalPreferences;

public class RoutePlannerActivity extends AppCompatActivity {

    private AutoCompleteTextView startLocationTextField = null;
    private AutoCompleteTextView stopLocationTextField = null;
    private ArrayAdapter<String> adapterAutocompleteFields;
    private static final String TAG = "BrasovBus_RouteCreator";
    private String requestToServer = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_route_planner);

        startLocationTextField = (AutoCompleteTextView) findViewById(R.id.startLocationTextField);
        stopLocationTextField = (AutoCompleteTextView) findViewById(R.id.stopLocationTextField);
        final Button preferencesButton = (Button) findViewById(R.id.preferencesButton);

        adapterAutocompleteFields = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, Constants.busStations);

        startLocationTextField.setThreshold(2);
        startLocationTextField.setAdapter(adapterAutocompleteFields);
        startLocationTextField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                stopLocationTextField.requestFocus();
            }
        });

        stopLocationTextField.setThreshold(2);
        stopLocationTextField.setAdapter(adapterAutocompleteFields);
        stopLocationTextField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                InputMethodManager inputMethodManager = (InputMethodManager)  RoutePlannerActivity.this.getSystemService(RoutePlannerActivity.this.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(RoutePlannerActivity.this.getCurrentFocus().getWindowToken(), 0);
            }
        });


        preferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent preferencesActivityIntent = new Intent(RoutePlannerActivity.this, PreferencesActivity.class);
                RoutePlannerActivity.this.startActivity(preferencesActivityIntent);
            }
        });

        Button okButton = (Button) findViewById(R.id.okButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String startStation = startLocationTextField.getText().toString();

                if (!Constants.busStations.contains(startStation)) {
                    Toast.makeText(RoutePlannerActivity.this, "Please select a valid start bus station!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String stopStation = stopLocationTextField.getText().toString();

                if (!Constants.busStations.contains(stopStation)) {
                    Toast.makeText(RoutePlannerActivity.this, "Please select a valid stop bus station!",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (startStation.equals(stopStation)) {
                    Toast.makeText(RoutePlannerActivity.this, "Unable to continue because the start and the stop stations are the same!",
                            Toast.LENGTH_LONG).show();
                    return;
                }


                //Create the request
                requestToServer = buildRequestForDS(startStation, stopStation);

                Log.i(TAG,"requestToServer: " + requestToServer);

                SendMessageToServer sendMessageTask = new SendMessageToServer();
                sendMessageTask.execute();

                Toast.makeText(RoutePlannerActivity.this,
                        "Please wait until the system determines the best route to travel.",
                        Toast.LENGTH_LONG).show();

            }
        });

        Log.i(TAG, "Application is started");
    }


    private class SendMessageToServer extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.i(TAG,"The following request was sent to the decision support ("
                    + Constants.DECISION_SUPPORT_END_POINT + "): " + requestToServer);


            WebSocketBasicClient webSocketBasicClient = new WebSocketBasicClient(
                    Constants.DECISION_SUPPORT_END_POINT, requestToServer);

            String decisionSupportResponse = webSocketBasicClient
                    .sendWebsocketRequest();

            Log.i(TAG,"The following response was received from the decision support: "
                    + decisionSupportResponse);

            if (decisionSupportResponse == null) {

                Intent errorActivityIntent = new Intent(RoutePlannerActivity.this, ErrorReportActivity.class);
                errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "The recommendation message is " +
                        "null. Please check if the decision support component is running at the " +
                        "following endpoint " + Constants.DECISION_SUPPORT_END_POINT);
                RoutePlannerActivity.this.startActivity(errorActivityIntent);
                Log.i(TAG,"The recommendation message is " +
                        "null. Please check if the decision support component is running at the " +
                        "following endpoint " + Constants.DECISION_SUPPORT_END_POINT);

            } else if (decisionSupportResponse.equals("{\"answers\":[]}")) {

                Intent errorActivityIntent = new Intent(RoutePlannerActivity.this, ErrorReportActivity.class);
                errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "The decision support component " +
                        "is working but is not able to provide a recommendation. The current decision "
                        + "support endpoint is " + Constants.DECISION_SUPPORT_END_POINT);
                RoutePlannerActivity.this.startActivity(errorActivityIntent);
                Log.i(TAG,"The decision support component " +
                        "is working but is not able to provide a recommendation. The current decision "
                        + "support endpoint is " + Constants.DECISION_SUPPORT_END_POINT);

            } else {
                //start route selection intent.
                Intent routeActivityIntent = new Intent(RoutePlannerActivity.this, RoutesSelectionActivity.class);
                routeActivityIntent.putExtra("decisionSupportResponse", decisionSupportResponse);
                RoutePlannerActivity.this.startActivity(routeActivityIntent);
            }
            return null;
        }
    }

    private String buildRequestForDS(String startStation, String stopStation) {

        //getting preferences set in the app settings
        SharedPreferences app_preferences =
                PreferenceManager.getDefaultSharedPreferences(RoutePlannerActivity.this);

        boolean fastestRouteCheckbox = app_preferences.getBoolean(Constants.FASTEST, Constants.FASTEST_INITIAL_VALUE);
        boolean minBusStationsCheckbox = app_preferences.getBoolean(Constants.MIN_BUS_STOPS, Constants.MIN_BUS_STOPS_INITIAL_VALUE);
        boolean busWithDisabilityFacilities = app_preferences.getBoolean(Constants.BUS_WITH_DISABILITY, Constants.BUS_WITH_DISABILITY_INITIAL_VALUE);

        Log.i(TAG,"fastestRouteCheckbox: "
                + fastestRouteCheckbox);
        Log.i(TAG,"minBusStationsCheckbox: "
                + minBusStationsCheckbox);
        Log.i(TAG,"busWithDisabilityFacilities: "
                + busWithDisabilityFacilities);

        final FunctionalParameters functionalParameters = new FunctionalParameters();

        functionalParameters.addFunctionalParameter(new FunctionalParameter(
                FunctionalParameterName.STARTING_POINT,
                new StringFunctionalParameterValue(getBusStationCoordinates(startStation))));
        functionalParameters.addFunctionalParameter(new FunctionalParameter(
                FunctionalParameterName.ENDING_POINT,
                new StringFunctionalParameterValue(getBusStationCoordinates(stopStation))));
        functionalParameters.addFunctionalParameter(new FunctionalParameter(
                FunctionalParameterName.TRANSPORTATION_TYPE,
                new StringFunctionalParameterValue("car")));
        functionalParameters.addFunctionalParameter(new FunctionalParameter(
                FunctionalParameterName.STARTING_DATETIME,
                new StringFunctionalParameterValue(String.valueOf(System.currentTimeMillis()))));

        final FunctionalConstraints functionalConstraints = new FunctionalConstraints();
        if(busWithDisabilityFacilities) {
            functionalConstraints.addFunctionalConstraint(new FunctionalConstraint(
                    FunctionalConstraintName.COST,
                    FunctionalConstraintOperator.EQUAL,
                    new IntegerFunctionalConstraintValue(1)));
        }

        final FunctionalPreferences functionalPreferences = new FunctionalPreferences();
        if(minBusStationsCheckbox) {
            functionalPreferences.addFunctionalPreference(new FunctionalPreference(
                    1, FunctionalPreferenceOperation.MINIMIZE,
                    FunctionalConstraintName.DISTANCE));
        }
        if(fastestRouteCheckbox) {
            functionalPreferences.addFunctionalPreference(new FunctionalPreference(
                    2, FunctionalPreferenceOperation.MINIMIZE,
                    FunctionalConstraintName.TIME));
        }

        final ReasoningRequest reasoningRequest = new ReasoningRequest(
                new User(), ARType.TRAVEL_PLANNER, new FunctionalDetails(
                functionalParameters, functionalConstraints,
                functionalPreferences));


        final GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(FunctionalParameterValue.class,
                new FunctionalParameterValueAdapter());
        builder.registerTypeAdapter(FunctionalConstraintValue.class,
                new FunctionalConstraintValueAdapter());
        builder.registerTypeAdapter(Answer.class, new AnswerAdapter());

        final Gson gson = builder.create();

        Log.i(TAG,"reasoningRequest: "
                + reasoningRequest.toString());

        final String rrString = gson.toJson(reasoningRequest);

        return rrString;
    }

    private String getBusStationCoordinates(String busStation) {

        int indexOfStation = Constants.busStations.indexOf(busStation);
        Coordinate coordinate = Constants.busStationCoordinates.get(indexOfStation);
        String coordinateString = coordinate.getLongitude() + " " + coordinate.getLatitude();

        return coordinateString;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finish();
    }
}



