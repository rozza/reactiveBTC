package com.example.reactiveBTC;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.mongodb.Document;
import org.mongodb.MongoClientOptions;
import org.mongodb.MongoClientURI;
import org.mongodb.async.MongoClient;
import org.mongodb.async.MongoClients;
import org.mongodb.async.MongoCollection;
import org.mongodb.operation.QueryFlag;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.concurrent.Future;

/**
 * Tail Trades data service
 *
 * Reads data from the trades capped collection and then notifies the trades websocket.
 *
 * MongoDB URI can be set via the `uri` system property (defaults to mongodb://localhost:27017)
 */
public class TailTrades {
    private MongoCollection<Document> collection;
    private final long startTime = System.currentTimeMillis();
    private String uri;

    public static void main(String[] args) {
        try {
            new TailTrades(System.getProperty("uri", "mongodb://localhost:27017"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TailTrades(String uri) throws UnknownHostException {
        this.uri = uri;
        collection = setupMongoDB();
        tailAndNotify();

        boolean stop = false;
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stop = true;
            }
        }
    }

    /**
     * We loop here as if the cursor closes (eg there are no results) we still want future results
     */
    private void tailAndNotify() {
        System.out.println("Tailing the capped collection and emitting notifications");
        collection
                .find(new Document())
                .cursorFlags(EnumSet.of(QueryFlag.Tailable, QueryFlag.AwaitData))
                .forEach(document -> {
                    System.out.println(String.format("[%d] MongoDB New Data: %s", timestamp(), document));
                    notifyWebSockets(document.toString()
                    );
                }).register((result, e) -> {
                    // Pause and retry the cursor may have been empty.
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    tailAndNotify();
                });
    }
    private MongoCollection<Document> setupMongoDB() throws UnknownHostException {
        MongoClientURI clientURI = new MongoClientURI(uri);
        MongoClient mongoClient = MongoClients.create(clientURI, MongoClientOptions.builder().build());
        return mongoClient.getDatabase("reactiveBTC").getCollection("trades");
    }


    private void notifyWebSockets(final String data) {
        URI uri = URI.create("ws://localhost:8080/trades/");

        WebSocketClient client = new WebSocketClient();
        try {
            try {
                client.start();
                // The socket that receives events
                TradeSocket socket = new TradeSocket();
                // Attempt Connect
                Future<Session> fut = client.connect(socket, uri);
                // Wait for Connect
                Session session = fut.get();
                // Send a message
                session.getRemote().sendString(data);
                // Close session
                session.close();
            } finally {
                client.stop();
            }
        } catch (Throwable t) {
            //t.printStackTrace(System.err);
        }
    }


    private long timestamp() {
        return System.currentTimeMillis() - startTime;
    }
}

