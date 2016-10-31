package com.siemens.ct.citypulse.brasovbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import citypulse.commons.data.Coordinate;
import citypulse.commons.reasoning_request.Answer;
import citypulse.commons.reasoning_request.Answers;
import citypulse.commons.reasoning_request.concrete.AnswerAdapter;
import citypulse.commons.reasoning_request.concrete.AnswerTravelPlanner;

public class RoutesSelectionActivity extends AppCompatActivity {

    private static final String TAG = "BrasovBus_RouteCreator";
    private ListView resultsListView;
    private int selectedItemNumber = -1;
    private LinkedList<CompleteBusStationDetails> processedAnswers = new LinkedList<CompleteBusStationDetails>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes_selection);

        resultsListView = (ListView) findViewById(R.id.resultsListView);

        List<AnswerTravelPlanner> listOfAnswersFromDS = new ArrayList<AnswerTravelPlanner>();
        List<Map<String, String>> dataToDisplay = new ArrayList<Map<String, String>>();

        //get the extra
        Bundle extras = getIntent().getExtras();

        final GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Answer.class,
                new AnswerAdapter());

        final Gson gson = builder.create();

        Type type = new TypeToken<Answers>() {
        }.getType();

        Answers answers = gson.fromJson(extras.getString("decisionSupportResponse"),type);

        for(Answer answer : answers.getAnswers())
        {
            listOfAnswersFromDS.add(new Gson().fromJson(answer.toJSON(), AnswerTravelPlanner.class));
        }


        //populate the dataToDisplay list that will be used in the app interface
        for (AnswerTravelPlanner answer : listOfAnswersFromDS) {

            CompleteBusStationDetails completeBusStationDetails = new CompleteBusStationDetails();
            LinkedHashMap<String, LinkedList<BusStation>> busNumberAndListOfStationsLinker = new LinkedHashMap<String, LinkedList<BusStation>>();
            LinkedList<BusStation> listOfStationsLinker = new LinkedList<BusStation>();

            String stringListOfBusesOnRoute = answer.getBus_route();
            List<String> listOfBusesOnRoute = Arrays.asList(stringListOfBusesOnRoute.split("\\s*;\\s*")); //stringListOfBusesOnRoute.split(",")


            List<Coordinate> routeCoordinates = answer.getRoute();

            Coordinate previousCoordinate = new Coordinate(0.0, 0.0);
            int busNumberIndex = 0;

            for (Coordinate coordinate : routeCoordinates) {

                //if the current station is the same as the previous one, it means that we hav ea bus change
                if (coordinate.getLongitude() == previousCoordinate.getLongitude() && coordinate.getLatitude() == previousCoordinate.getLatitude()) {

                    //we add the current bus number as key in the map and the list of stations as value
                    busNumberAndListOfStationsLinker.put(listOfBusesOnRoute.get(busNumberIndex), listOfStationsLinker);

                    //we clear the String list of stations for the next bus number
                    listOfStationsLinker = new LinkedList<>();
                    busNumberIndex++;
                }

                //add the station names to a list of stations
                listOfStationsLinker.add(getBusStationDetails(coordinate));

                //update the last coordinates
                previousCoordinate = coordinate;

                //if this is the last stations, we append to the latest bus number and it's bus stations to the map
                if (routeCoordinates.indexOf(coordinate) == routeCoordinates.size()-1) {
                    busNumberAndListOfStationsLinker.put(listOfBusesOnRoute.get(busNumberIndex), listOfStationsLinker);
                    listOfStationsLinker = new LinkedList<>();
                }
            }

            //Log.i(TAG,"##busNumberAndListOfStationsLinker: "+busNumberAndListOfStationsLinker);

            completeBusStationDetails.setAnswerFromDS(answer);
            completeBusStationDetails.setBusNumberAndStationsLinker(busNumberAndListOfStationsLinker);

            //adding the map to the list
            processedAnswers.add(completeBusStationDetails);
        }

        int currentRouteIndexForActivityInterface = 1;
        for (CompleteBusStationDetails completeBusStationDetails : processedAnswers) {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("title", "Ruta " + currentRouteIndexForActivityInterface + ": aprox. " + completeBusStationDetails.getAnswerFromDS().getNumber_of_seconds() + " sec");
            datum.put("description", completeBusStationDetails.toString());

            dataToDisplay.add(datum);
            currentRouteIndexForActivityInterface++;
        }

        Log.i(TAG,"##dataToDisplay: "+dataToDisplay);

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedItemNumber = i;

            }
        });

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, dataToDisplay,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "description"},
                new int[]{android.R.id.text1,
                        android.R.id.text2});
        resultsListView.setAdapter(simpleAdapter);

        Button selectRouteButton = (Button) findViewById(R.id.selectRoutebutton);

        selectRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (selectedItemNumber == -1) {

                    Toast.makeText(RoutesSelectionActivity.this,
                            "Please select a route.",
                            Toast.LENGTH_LONG).show();

                    return;
                }

                //get the corresponding stations string from the Map
                Intent navigationActivityIntent = new Intent(RoutesSelectionActivity.this, NavigationActivity.class);
                navigationActivityIntent.putExtra("selectedRoute", new Gson().toJson(processedAnswers.get(selectedItemNumber)));
                RoutesSelectionActivity.this.startActivity(navigationActivityIntent);

                finish();
            }
        });
    }


    private BusStation getBusStationDetails(Coordinate currentStationCoordinates) {

        BusStation busStation = new BusStation();

        for (Coordinate coord : Constants.busStationCoordinates) {

            if (coord.getLatitude() == currentStationCoordinates.getLatitude() && coord.getLongitude() == currentStationCoordinates.getLongitude()) {

                int index = Constants.busStationCoordinates.indexOf(coord);

                busStation.setCoordinate(coord);
                busStation.setBusStationName(Constants.busStations.get(index));
                busStation.setUuid(Constants.busStationsUUIDs.get(index));

                return busStation;
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finish();
    }
}
