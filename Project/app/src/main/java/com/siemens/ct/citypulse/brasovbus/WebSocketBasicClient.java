package com.siemens.ct.citypulse.brasovbus;

import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

public class WebSocketBasicClient {

	private static final String TAG = "BrasovBus_WebSocket";

	String webSocketEndPoint;
	String payload;
	String webSocketResponse = null;

	public WebSocketBasicClient(String webSocketEndPoint, String payload) {
		super();
		this.webSocketEndPoint = webSocketEndPoint;
		this.payload = payload;
	}

	public String sendWebsocketRequest() {

		final CountDownLatch messageLatch = new CountDownLatch(1);

		ClientEndpointConfig cec = ClientEndpointConfig.Builder.create()
				.build();

		ClientManager clientManager = ClientManager.createClient();

		Endpoint clientEndpoint = new Endpoint() {

			@Override
			public void onOpen(Session session, EndpointConfig config) {

				session.addMessageHandler(new MessageHandler.Whole<String>() {

					public void onMessage(String message) {

						webSocketResponse = message;

						messageLatch.countDown();

					}

				});

				try {
					session.getBasicRemote().sendText(payload);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};

		try {
			Session session = clientManager.connectToServer(clientEndpoint, cec, new URI(
					webSocketEndPoint));
			
			session.setMaxIdleTimeout(Constants.WEB_SOCKET_MAXIMUM_IDLE_TIMEOUT);

			long initial_time = System.currentTimeMillis();
			messageLatch.await(Constants.WEB_SOCKET_RESPONSE_WAITING_TIME_IN_SECONDS, TimeUnit.SECONDS);
			
			Log.i(TAG,"Waiting time time for the response " + (System.currentTimeMillis() - initial_time));
			
			session.close();
			


		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return webSocketResponse;

	}

}
