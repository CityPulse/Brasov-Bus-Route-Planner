# Brasov-Route-Planner

In order to demonstrate how the CityPulse framework can be used to develop applications for smart cities and citizens, we have implemented a context-aware real time Travel Planner using the live data from the city of Brasov, Romania. This scenario aims to provide bus travel-planning solutions, which go beyond the state of the art solutions by allowing users to provide multi dimensional requirements and preferences such as the fastest route, number of buses, minimum number of stops as well as busses with special facilities for people with disabilities. In this way the users receive bus route recommendations based on the current context of the city. In addition to this, Travel Planner continuously monitors the user context and events detected on the planned route. User will be prompted to opt for a different route if there are reported incidents ahead. All the CityPulse framework components are deployed on a back-end server and are accessible via a set of APIs. As a result of that the application developer has only to develop a user-friendly front-end application, which calls the framework APIs. In our case we have developed an Android application.

## Application UI

Figure 1 depict the user interfaces used by the end user to set the travel preferences and the destination start point and end point.

<img src="https://github.com/CityPulse/Brasov-Bus-Route-Planner/blob/master/Images/Screenshot_2016-11-16-15-20-13.png" width="276" height="489" alt="Figure 1 a) - The user interfaces of the Android application used to select the starting point and ending point" />
<img src="https://github.com/CityPulse/Brasov-Bus-Route-Planner/blob/master/Images/Screenshot_2016-11-16-15-20-24.png" width="276" height="489" alt="Figure 1 b) - Travel preferences"/>

After the user has filled in the details and made the request using the user interface, the mobile application generates the appropriate request for the Decision support component which has the following main fields:

* Type: indicating what decision support module is to be used for this application (“TRAVEL-PLANNER” in this case);
* Functional details: specifying possible values of user’s requirements, including: 
 * Functional parameters: mandatory information that the user provides such as starting and ending locations. 
 * Functional constraints: the user can filter buses that are equiped with facilities for people with disabilities. 
 * Functional preferences: the user can specify his preferences along selected routes, which hold the functional constraints. These preferences can be the minimization or the maximisation of travel time, number of bus stations or distance.

The functional constraints and preferences specify different thresholds and minimization criteria for electing the route. During the development of the mobile application the domain expert has computed a default set of values for these thresholds. As a result of that, the route constraints user interface from Figure 1 b) allows the user to select between the fastest/shortest routes. If needed, more fields can be added in this user interface in order to allow more fine-grained constraints specification, but the usability of the application may suffer.

<img src="https://github.com/CityPulse/Brasov-Bus-Route-Planner/blob/master/Images/Screenshot_2016-11-17-16-23-09.png" width="276" height="489" alt="Figure 2 a) - The user interfaces of the Android application while selecting the preferred route" />
<img src="https://github.com/CityPulse/Brasov-Bus-Route-Planner/blob/master/Images/Screenshot_2016-11-16-15-20-24.png" width="276" height="489" alt="Figure 2 a) - Notification of an incident which appeared on the selected route while the user is travelling"/>

After the user selects the preferred route, a request is generated to the Contextual filtering component in order to identify the relevant events for the use while he/she is traveling. 

Once the user has selected one of the routes computed by the Decision support component, the Contextual Filtering component subscribes to the event streams on the route via a Data Bus in order to detect eventual incidents. Whenever there is a new event detected by the Event detection component, the Contextual Filtering component filters and assigns the most appropriate criticality (0 if not critical, from 1 to 5 if it is critical) to the new event. If the new event is marked as critical, the user receives a notification and he/she has the option to change the current solution and request a new one or ignore the event. 

Figure 2 b) depicts the notification received by the end user, while she/he is traveling and an incident is detected on his/hers route.


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
