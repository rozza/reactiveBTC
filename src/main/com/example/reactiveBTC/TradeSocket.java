package com.example.reactiveBTC;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@ClientEndpoint
@ServerEndpoint(value="/trades/")
public class TradeSocket
{
    private final static ConcurrentHashMap<String, Session> sockets = new ConcurrentHashMap<>();

    @OnOpen
    public void onWebSocketConnect(Session sess)
    {
        sockets.put(sess.getId(), sess);
        System.out.println("Socket Connected: " + sess);
    }

    @OnMessage
    public void onWebSocketText(String message)
    {
        for (Session sess: sockets.values()){
            if (!sess.isOpen()) {
                sockets.remove(sess.getId());
            } else {
                try {
                    sess.getAsyncRemote().sendText(message).get(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason)
    {
        System.out.println("Socket Closed: " + reason);
    }

    @OnError
    public void onWebSocketError(Throwable cause)
    {
        cause.printStackTrace(System.err);
    }

    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return null;
    }
}
