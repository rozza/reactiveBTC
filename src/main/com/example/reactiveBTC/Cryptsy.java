package com.example.reactiveBTC;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionStateChange;
import org.eclipse.jetty.util.component.LifeCycle;
import org.mongodb.CreateCollectionOptions;
import org.mongodb.Document;
import org.mongodb.MongoClientOptions;
import org.mongodb.MongoClientURI;
import org.mongodb.async.MongoClient;
import org.mongodb.async.MongoClients;
import org.mongodb.async.MongoCollection;
import org.mongodb.async.MongoDatabase;
import org.mongodb.codecs.DocumentCodec;
import org.mongodb.json.JSONReader;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Cryptsy implements ConnectionEventListener, ChannelEventListener {

    private final long startTime = System.currentTimeMillis();
    private final DocumentCodec codec = new DocumentCodec();
    private final MongoCollection collection;
    Boolean stop = false;

    public static void main(String[] args) {
        try {
            new Cryptsy();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Cryptsy() throws UnknownHostException {

        this.collection = setupMongoDB();

        String apiKey = "cb65d0a7a72cd94adf1f";
        String eventName = "message";

        PusherOptions options = new PusherOptions().setEncrypted(true);
        Pusher pusher = new Pusher(apiKey, options);
        pusher.connect(this);

        // Subscribe to tickers
        pusher.subscribe("ticker.3", this, eventName);   // LTC / BTC
        pusher.subscribe("ticker.132", this, eventName); // DOGE / BTC
        pusher.subscribe("ticker.155", this, eventName); // DRK / BTC

        // Keep main thread asleep while we watch for events or application will terminate
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /* ConnectionEventListener implementation */
    @Override
    public void onConnectionStateChange(ConnectionStateChange change) {

        System.out.println(String.format(
                "[%d] Connection state changed from [%s] to [%s]",
                timestamp(), change.getPreviousState(), change.getCurrentState()));
    }

    @Override
    public void onError(String message, String code, Exception e) {

        System.out.println(String.format(
                "[%d] An error was received with message [%s], code [%s], exception [%s]",
                timestamp(), message, code, e));
    }

    /* ChannelEventListener implementation */

    @Override
    public void onEvent(String channelName, String eventName, String data) {

        System.out.println(String.format(
                "[%d] Received event [%s] on channel [%s] with data [%s]",
                timestamp(), eventName, channelName, data));

        Document doc = codec.decode(new JSONReader(data));
        collection.insert(doc).register((writeResult, e) -> notifyWebSockets(data));
    }

    @Override
    public void onSubscriptionSucceeded(String channelName) {

        System.out.println(String.format(
                "[%d] Subscription to channel [%s] succeeded",
                timestamp(), channelName));
    }

    private void notifyWebSockets(String data) {
        URI uri = URI.create("ws://localhost:8080/trades/");

        try
        {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            try
            {
                // Attempt Connect
                Session session = container.connectToServer(TradeSocket.class, uri);
                // Send a message
                session.getAsyncRemote().sendText(data).get(100, TimeUnit.MILLISECONDS);
                // Close session
                session.close();
            }
            finally
            {
                // Force lifecycle stop when done with container.
                // This is to free up threads and resources that the
                // JSR-356 container allocates. But unfortunately
                // the JSR-356 spec does not handle lifecycles (yet)
                if (container instanceof LifeCycle)
                {
                    ((LifeCycle)container).stop();
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }

    private long timestamp() {
        return System.currentTimeMillis() - startTime;
    }

    private MongoCollection setupMongoDB() throws UnknownHostException {
        MongoClientURI uri = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = MongoClients.create(uri, MongoClientOptions.builder().build());
        MongoDatabase db = mongoClient.getDatabase("reactiveBTC");
        db.getCollection("trades").tools().drop().get();
        db.tools().createCollection(new CreateCollectionOptions("trades", true, 1024)).get();

        return mongoClient.getDatabase("reactiveBTC").getCollection("trades");
    }
}
