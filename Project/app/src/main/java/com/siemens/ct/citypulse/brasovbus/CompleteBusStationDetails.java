package com.siemens.ct.citypulse.brasovbus;

import android.util.Log;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import citypulse.commons.reasoning_request.concrete.AnswerTravelPlanner;

/**
 * Class used to store the complete description of bus stations to be later send to the NavigationActivity.class
 * where all the informations will com in handy for generating the requests for Contextual Filtering
 *
 * Created by z003jn8y on 22.09.2016.
 */
public class CompleteBusStationDetails {

    /**
     * OrderedMap that contains the link between the bus number and the list of stations
     * representing the route that particular bus it taking
     */
    private LinkedHashMap<String, LinkedList<BusStation>> busNumberAndStationsLinker;
    /**
     * Object that holds information about the duration and length of the route, and the route itself
     * that will be used when generating the request for CF
     */
    private AnswerTravelPlanner answerFromDS;

    public CompleteBusStationDetails() {
    }

    public AnswerTravelPlanner getAnswerFromDS() {
        return answerFromDS;
    }

    public void setAnswerFromDS(AnswerTravelPlanner answerFromDS) {
        this.answerFromDS = answerFromDS;
    }

    public Map<String, LinkedList<BusStation>> getBusNumberAndStationsLinker() {
        return busNumberAndStationsLinker;
    }

    public void setBusNumberAndStationsLinker(LinkedHashMap<String, LinkedList<BusStation>> busNumberAndStationsLinker) {
        this.busNumberAndStationsLinker = busNumberAndStationsLinker;
    }

    @Override
    public String toString() {

        String returnedString = "";

        for(String busNumber : busNumberAndStationsLinker.keySet())
        {
            //Log.i("BrasovBus_RouteCreator","##busNumber in LIST## -> "+busNumber);
            returnedString = returnedString + "Autobuz " + busNumber + ": ";

            for(BusStation busStation : busNumberAndStationsLinker.get(busNumber))
            {
                if(busNumberAndStationsLinker.get(busNumber).indexOf(busStation) == 0)
                {
                    returnedString = returnedString + busStation.getBusStationName();
                }
                else
                {
                    returnedString = returnedString + ", " + busStation.getBusStationName();
                }
            }
            returnedString = returnedString + "\n";
        }

        return returnedString;
    }
}
