# Brasov-Route-Planner

In order to demonstrate how the CityPulse framework can be used to develop applications for smart cities and citizens, we have implemented a context-aware real time Travel Planner using the live data from the city of Brasov, Romania. This scenario aims to provide bus travel-planning solutions, which go beyond the state of the art solutions by allowing users to provide multi dimensional requirements and preferences such as the fastest route, number of buses, minimum number of stops as well as busses with special facilities for people with disabilities. In this way the users receive bus route recommendations based on the current context of the city. In addition to this, Travel Planner continuously monitors the user context and events detected on the planned route. User will be prompted to opt for a different route if there are reported incidents ahead. All the CityPulse framework components are deployed on a back-end server and are accessible via a set of APIs. As a result of that the application developer has only to develop a user-friendly front-end application, which calls the framework APIs. In our case we have developed an Android application.

## Application UI

Figure 1 depict the user interfaces used by the end user to set the travel preferences and the destination start point and end point.

![alt text](https://github.com/CityPulse/Brasov-Bus-Route-Planner/blob/master/Images/Screenshot_2016-11-16-15-20-13.png "Figure 1 a)- The user interfaces of the Android application used to select the starting point and the ending point")

## Dependencies

In order to allow the application developer to use Google Maps services, he/she will need to follow there steps:

* Import the project in his/hers preffered IDE.
* Edit the AndroidManifest.xml file al line 53(the key to be more exact)
* In order to obain the new keys, we will need to get the SHA1 key from his IDE and go to https://console.developers.google.com/home/dashboard to get a new key. This will allow him to run the application while still connected via USB cable to the computer.

The application developer also has to edit the Constants.java . He will need to adapt to his scenario the Decision Support, Contextual Fintering and Traffic incidents reporting endpoint URLs.(lines 34,36,38 in Constants.java)
  
## Contributers

The Brasov-Route-Planner was developed as part of the EU project CityPulse. The consortium member SIEMENS Corporate Technology Romania provided the main contributions for this component.

CityPulse: http://www.ict-citypulse.eu/

SIEMENS Corporate Technology Romania: http://www.siemens.ro

## Authors

* **Dan Puiu**
* **Serbanescu Bogdan**
