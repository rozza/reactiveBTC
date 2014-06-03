package com.example.reactiveBTC;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionStateChange;
import org.mongodb.Document;
import org.mongodb.MongoClientOptions;
import org.mongodb.MongoClientURI;
import org.mongodb.async.MongoClient;
import org.mongodb.async.MongoClients;
import org.mongodb.async.MongoCollection;
import org.mongodb.codecs.DocumentCodec;
import org.mongodb.json.JSONReader;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;

public class Cryptsy implements ConnectionEventListener, ChannelEventListener {

    private String eventName = "message";
    private final long startTime = System.currentTimeMillis();
    private final DocumentCodec codec = new DocumentCodec();
    private final MongoCollection<Document> collection;
    private Boolean stop = false;
    private String uri;

    public static void main(String[] args) {
        try {
            boolean simulate = Boolean.parseBoolean(System.getProperty("simulate", "false"));
            new Cryptsy(simulate, System.getProperty("uri", "mongodb://localhost:27017"));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Cryptsy(boolean simulate, String uri) throws UnknownHostException {
        this.uri = uri;
        this.collection = setupMongoDB();
        if (simulate) {
            simulate();
        } else {
            subscribe();
        }
    }

    private void simulate() {
        System.out.println("========= Simulating BTC prices =========");
        String simpleData = "{\"channel\":\"%s\",\"trade\":{\"topbuy\":{\"price\":\"%s\"}}}";
        HashMap<String, double[]> tickers = new HashMap<>();
        tickers.put("ticker.3", new double[]{0.016, 0.018});
        tickers.put("ticker.132", new double[]{0.00003, 0.00007});
        tickers.put("ticker.155", new double[]{0.020, 0.022});

        while (!stop) {
            for (String channel: tickers.keySet()) {
                double[] range = tickers.get(channel);
                double topbuy = randomInRange(range[0], range[1]);
                long sleep = (long) randomInRange(250, 1000);
                onEvent(channel, eventName, String.format(simpleData, channel, topbuy));
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep((long) randomInRange(1000, 2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void subscribe() {
        String apiKey = "cb65d0a7a72cd94adf1f";

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
        collection.insert(doc);
    }

    @Override
    public void onSubscriptionSucceeded(String channelName) {

        System.out.println(String.format(
                "[%d] Subscription to channel [%s] succeeded",
                timestamp(), channelName));
    }

    private long timestamp() {
        return System.currentTimeMillis() - startTime;
    }

    private MongoCollection<Document> setupMongoDB() throws UnknownHostException {
        MongoClientURI clientURI = new MongoClientURI(this.uri);
        MongoClient mongoClient = MongoClients.create(clientURI, MongoClientOptions.builder().build());
        return mongoClient.getDatabase("reactiveBTC").getCollection("trades");
    }

    private static Random random = new Random();
    private static double randomInRange(double min, double max) {
        double range = max - min;
        double scaled = random.nextDouble() * range;
        return scaled + min; // == (rand.nextDouble() * (max-min)) + min;
    }
}
