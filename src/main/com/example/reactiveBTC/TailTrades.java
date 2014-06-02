package com.example.reactiveBTC;

import org.eclipse.jetty.util.component.LifeCycle;
import org.mongodb.Document;
import org.mongodb.MongoClientOptions;
import org.mongodb.MongoClientURI;
import org.mongodb.async.MongoClient;
import org.mongodb.async.MongoClients;
import org.mongodb.async.MongoCollection;
import org.mongodb.async.MongoView;
import org.mongodb.operation.QueryFlag;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;


public class TailTrades {
    private final long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        try {
            new TailTrades();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TailTrades() throws UnknownHostException {
        MongoClientURI uri = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = MongoClients.create(uri, MongoClientOptions.builder().build());
        MongoCollection<Document> collection = mongoClient.getDatabase("reactiveBTC").getCollection("trades");

        while (true) {
            MongoView<Document> cursor = collection.find(new Document()).cursorFlags(EnumSet.of(QueryFlag.Tailable));
            cursor.forEach(document -> {
                System.out.println(String.format("[%d] new data in mongodb - %s", timestamp(), document));
                notifyWebSockets(document.toString());
            });
        }

    }

//    private void notifyWebSockets(String data) {
//        URI uri = URI.create("ws://localhost:8080/trades/");
//
//        WebSocketClient client = new WebSocketClient();
//        try
//        {
//            try
//            {
//                client.start();
//                // The socket that receives events
//                TradeSocket socket = new TradeSocket();
//                // Attempt Connect
//                Future<Session> fut = client.connect(socket, uri);
//                // Wait for Connect
//                Session session = fut.get();
//                // Send a message
//                session.getRemote().sendString(data);
//                // Close session
//                session.close();
//            }
//            finally
//            {
//                client.stop();
//            }
//        }
//        catch (Throwable t)
//        {
//            t.printStackTrace(System.err);
//        }
//    }

    private void notifyWebSockets(String data) {
        URI uri = URI.create("ws://localhost:8080/trades/");



        try {
            System.out.println("++++++++++++++++");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            System.out.println("++++++++++++++++");

            try {
                // Attempt Connect
                Session session = container.connectToServer(TradeSocket.class, uri);
                System.out.println("++++++++++++++++");
                System.out.println(session.isOpen());
                System.out.println("++++++++++++++++");


                // Send a message
                session.getAsyncRemote().sendText(data).get(100, TimeUnit.MILLISECONDS);
                // Close session
                session.close();
            } finally {
                // Force lifecycle stop when done with container.
                // This is to free up threads and resources that the
                // JSR-356 container allocates. But unfortunately
                // the JSR-356 spec does not handle lifecycles (yet)
                if (container instanceof LifeCycle) {
                    ((LifeCycle) container).stop();
                }
            }
        } catch (Throwable t) {
            //t.printStackTrace(System.err);
        }
    }

    private long timestamp() {
        return System.currentTimeMillis() - startTime;
    }
}
