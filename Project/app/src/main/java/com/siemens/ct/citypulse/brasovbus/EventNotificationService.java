package com.siemens.ct.citypulse.brasovbus;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.tyrus.client.ClientManager;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import citypulse.commons.contextual_filtering.contextual_event_request.ContextualEventRequest;
import citypulse.commons.contextual_filtering.contextual_event_request.FilteringFactor;
import citypulse.commons.contextual_filtering.contextual_event_request.FilteringFactorName;
import citypulse.commons.contextual_filtering.contextual_event_request.FilteringFactorValue;
import citypulse.commons.contextual_filtering.contextual_event_request.Place;
import citypulse.commons.contextual_filtering.contextual_event_request.PlaceAdapter;
import citypulse.commons.contextual_filtering.contextual_event_request.PlaceType;
import citypulse.commons.contextual_filtering.contextual_event_request.RankingElement;
import citypulse.commons.contextual_filtering.contextual_event_request.RankingElementName;
import citypulse.commons.contextual_filtering.contextual_event_request.RankingElementValue;
import citypulse.commons.contextual_filtering.contextual_event_request.RankingFactor;
import citypulse.commons.contextual_filtering.contextual_event_request.RankingFactorType;
import citypulse.commons.contextual_filtering.contextual_event_request.Route;
import citypulse.commons.data.Coordinate;
import citypulse.commons.reasoning_request.concrete.AnswerTravelPlanner;


public class EventNotificationService extends Service {

	private static final String TAG = "BrasovBus_Notification";

	private Looper mServiceLooper;
	private NotificationHandler notificationHandler;

	private String request;

	private final int CONTINOUS_WEKSOCKET_SESION = -1;

	private boolean stopThread = false;

	Service currentService = this;

	CompleteBusStationDetails completeBusStationDetails;

	private Session session;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {

		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		mServiceLooper = thread.getLooper();
		notificationHandler = new NotificationHandler(mServiceLooper);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Bundle extras = intent.getExtras();

		//get extra
		Type type = new TypeToken<CompleteBusStationDetails>() {
        }.getType();
		completeBusStationDetails = new Gson().fromJson(extras.getString("cf_request"), type);
        //end get extra

		request = generateRequestCF(completeBusStationDetails);

		Log.i(TAG,"Started notification service for the following request "
						+ request);

		Message msg = notificationHandler.obtainMessage();
		msg.arg1 = startId;
		notificationHandler.sendMessage(msg);

		return START_STICKY;
	}

	private String generateRequestCF(CompleteBusStationDetails completeBusStationDetails) {

		/*
		 * Place of interest_ Route
		 */
		final Route route = new Route();
		route.setPlaceId(""+completeBusStationDetails.getAnswerFromDS().getId());
		route.setRoute(completeBusStationDetails.getAnswerFromDS().getRoute());
		route.setLength(completeBusStationDetails.getAnswerFromDS().getLength());
		route.setType(PlaceType.ROUTE);
		final Place place = route;

		String requestRoute = completeBusStationDetails.getAnswerFromDS().getRoute().toString().replace("\"","\\");
		Log.i(TAG,"requestRoute: " + requestRoute);


		/*
		 * Filtering factors
		 */
		final Set<FilteringFactor> filteringFactors = new HashSet<FilteringFactor>();


		final Set<FilteringFactorValue> filteringFactorValueActivity = new HashSet<FilteringFactorValue>();
		filteringFactorValueActivity
				.add(new FilteringFactorValue("CarCommute"));
		FilteringFactor filteringFactor = new FilteringFactor(FilteringFactorName.ACTIVITY,
				filteringFactorValueActivity);
		filteringFactors.add(filteringFactor);

		/*
		 * Ranking factor
		 */
		final Set<RankingElement> rankingElements = new HashSet<RankingElement>();
		rankingElements.add(new RankingElement(RankingElementName.DISTANCE,
				new RankingElementValue(70)));
		rankingElements.add(new RankingElement(RankingElementName.EVENT_LEVEL,
				new RankingElementValue(30)));

		final RankingFactor rankingFactor = new RankingFactor(
				RankingFactorType.LINEAR, rankingElements);

		/*
		 * create a ContextualEventRequest
		 */
		final ContextualEventRequest request = new ContextualEventRequest(
				place, filteringFactors, rankingFactor);

		/*
		 * Convert to Gson string
		 */
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Place.class, new PlaceAdapter());
		final Gson gson = builder.create();

		final String gsonStr = gson.toJson(request);

		return gsonStr;
	}

	@Override
	public void onDestroy() {

		if (notificationHandler != null) {
			notificationHandler.stopThread();
		}

		Log.i(TAG,"Stopped notification service for the following request "
						+ request);
	}

	private final class NotificationHandler extends Handler implements
			LocationListener {

		private Location lastLocation = null;

		public void stopThread() {

			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if (ActivityCompat.checkSelfPermission(EventNotificationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(EventNotificationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				// TODO: Consider calling
				//    ActivityCompat#requestPermissions
				// here to request the missing permissions, and then overriding
				//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
				//                                          int[] grantResults)
				// to handle the case where the user grants the permission. See the documentation
				// for ActivityCompat#requestPermissions for more details.
				Intent errorActivityIntent = new Intent(EventNotificationService.this, ErrorReportActivity.class);
				errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "Unable to start the event " +
						"notification service because the user has not provided permission to " +
						"use the GPS.");
				EventNotificationService.this.startActivity(errorActivityIntent);
				Log.i(TAG,"Unable to start the event notification service because the user has not " +
						"provided permission to use the GPS.");
				return;
			}
			locationManager.removeUpdates(this);

			stopThread = true;

			try {

				session.close(new CloseReason(new CloseCode() {
                    @Override
                    public int getCode() {
                        return 1000;
                    }
                }, "The user stopped the reasoning."));
				Log.i(TAG, "Session is not null and is going to close from stopThread().");
				session = null;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		public NotificationHandler(Looper looper) {
			super(looper);

			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if (ActivityCompat.checkSelfPermission(EventNotificationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(EventNotificationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				// TODO: Consider calling
				//    ActivityCompat#requestPermissions
				// here to request the missing permissions, and then overriding
				//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
				//                                          int[] grantResults)
				// to handle the case where the user grants the permission. See the documentation
				// for ActivityCompat#requestPermissions for more details.
				Intent errorActivityIntent = new Intent(EventNotificationService.this, ErrorReportActivity.class);
				errorActivityIntent.putExtra(Constants.ERROR_MESSAGE, "Unable to start the event " +
						"notification service because the user has not provided permission to " +
						"use the GPS.");
				EventNotificationService.this.startActivity(errorActivityIntent);
				Log.i(TAG,"Unable to start the event notification service because the user has not " +
						"provided permission to use the GPS.");
				return;
			}

			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 1000, 10, this);
		}

		@Override
		public void handleMessage(Message msg) {

			ClientEndpointConfig cec = ClientEndpointConfig.Builder.create()
					.build();

			ClientManager clientManager = ClientManager.createClient();


			Endpoint clientEndpoint = new Endpoint() {

				@Override
				public void onOpen(Session session, EndpointConfig config) {

					session.addMessageHandler(new MessageHandler.Whole<String>() {

						public void onMessage(String message) {

							Log.i(TAG,"Received contextual event "+ message);

							Intent intent = new Intent();
							intent.setAction(Constants.EVENT_ALERT_MESSAGE);
							intent.putExtra(
									Constants.EVENT_ALERT_MESSAGE_PAYLOAD,
									message);
							sendBroadcast(intent);

						}

					});

				}
			};


			String requestEndpoint = Constants.CONTEXTUAL_FILTERING_END_POINT;

			session = null;
			try {

				Log.i(TAG,"Connecting to contextual filtering  " + requestEndpoint);

				session = clientManager.connectToServer(clientEndpoint, cec,
						new URI(requestEndpoint));

				session.setMaxIdleTimeout(CONTINOUS_WEKSOCKET_SESION);

				session.getBasicRemote().sendText(request);

				while (!stopThread) {

					if (lastLocation != null) {
						//Compute the location status and send it to contextual filtering

//						session.getBasicRemote().sendText(
//								new Gson().toJson(event));

					}
					Thread.sleep(Constants.EVENT_NOTIFICATION_SERVICE_PERIOD);
				}

				if(session!=null) {
					Log.i(TAG, "Session is not null and is going to close.");

					session.close(new CloseReason(new CloseCode() {

						@Override
						public int getCode() {
							return 1000;
						}
					}, "The user stopped the reasoning."));
				}

			} catch (DeploymentException e) {
				Log.i(TAG,"Unable to open the connection with contextual filtering for parking  Deployment exception",e);

				Intent intent = new Intent();
				intent.setAction(Constants.ERROR_MESSAGE);
				intent.putExtra(Constants.ERROR_MESSAGE_PAYLOAD,
						"Unable to connect to the contextual filtering for parking module: "
								+ requestEndpoint);
				sendBroadcast(intent);

				e.printStackTrace();
			} catch (IOException e) {
				Log.i(TAG,"Unable to open the connection with contextual filtering for parking IO exception",e);

				Intent intent = new Intent();
				intent.setAction(Constants.ERROR_MESSAGE);
				intent.putExtra(Constants.ERROR_MESSAGE_PAYLOAD,
						"Unable to connect to the contextual filtering for parking module: "
								+ requestEndpoint);
				sendBroadcast(intent);

			} catch (URISyntaxException e) {
				Log.i(TAG,"Unable to open the connection with contextual filtering for parking URI sintax error",e);

				Intent intent = new Intent();
				intent.setAction(Constants.ERROR_MESSAGE);
				intent.putExtra(Constants.ERROR_MESSAGE_PAYLOAD,
						"Unable to connect to the contextual filtering for parking module: "
								+ requestEndpoint);
				sendBroadcast(intent);

			} catch (InterruptedException e) {
				Log.i(TAG,"Unable to open the connection with contextual filtering for parking URI InterruptedException",e);

                Intent intent = new Intent();
				intent.setAction(Constants.ERROR_MESSAGE);
				intent.putExtra(Constants.ERROR_MESSAGE_PAYLOAD,
						"Unable to connect to the contextual filtering for parking module: "
								+ requestEndpoint);
				sendBroadcast(intent);
			} catch (IllegalStateException e) {
				Log.i(TAG,"The connection to the internet was interupted while the app was connected to the server.",e);

                Intent intent = new Intent();
				intent.setAction(Constants.ERROR_MESSAGE);
				intent.putExtra(
						Constants.ERROR_MESSAGE_PAYLOAD,
						"The connection to the internet was interupted while "
								+ "the app was connected to the server for receiving parking events. Most probably the "
								+ "device was disconnect from the internet. You have to "
								+ "request a new recomendation, because you will not receive "
								+ "any updates from the previous one. The server location is: "
								+ requestEndpoint);
				sendBroadcast(intent);
			}

		}

		@Override
		public void onLocationChanged(Location location) {
			lastLocation = location;
			calculateClosestStation();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}

		private void calculateClosestStation() {

			double minDistance = 100;
			String closestStationName = null;

			double latitude = lastLocation.getLatitude();
			double longitude = lastLocation.getLongitude();

			for(String busNumber : completeBusStationDetails.getBusNumberAndStationsLinker().keySet())
			{
				LinkedList<BusStation> listOfStations = completeBusStationDetails.getBusNumberAndStationsLinker().get(busNumber);
				for(BusStation bs : listOfStations)
				{

					//get distance
					double calculatedDistance = Math.sqrt(Math.pow((bs.getCoordinate().getLatitude()-latitude),2)+Math.pow((bs.getCoordinate().getLongitude()-longitude),2));

					if(minDistance>calculatedDistance)
					{
						minDistance = calculatedDistance;
						closestStationName = bs.getBusStationName();
					}
				}
			}

			Log.i(TAG,"Location changed, current station: "+closestStationName);
			Intent intent = new Intent();
			intent.setAction(Constants.LOCATION_UPDATE_MESSAGE);
			intent.putExtra(
					Constants.LOCATION_UPDATE_MESSAGE_PAYLOAD,
					closestStationName);
			sendBroadcast(intent);

		}
	}
}
