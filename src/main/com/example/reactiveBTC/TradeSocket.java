package com.example.reactiveBTC;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketException;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The TradeSocket web sockets
 *
 * Caches the open sockets and on receiving of data notifies the existing sessions.
 */
public class TradeSocket extends WebSocketAdapter
{

    private final static ConcurrentHashMap<String, Session> sockets = new ConcurrentHashMap<>();

    @Override
    public void onWebSocketConnect(Session session)
    {
        super.onWebSocketConnect(session);
        String host = getSession().getRemoteAddress().toString();
        sockets.put(host, session);
        System.out.println("Socket Connected: " + host);
    }

    @Override
    public void onWebSocketText(String message)
    {
        for (Session sess: sockets.values()){
            if (!sess.isOpen()) {
                sockets.remove(sess.getRemoteAddress().toString());
            } else {
                try {
                    sess.getRemote().sendString(message);
                } catch (IOException | WebSocketException | IllegalStateException e) {
                    // continue it may be closing / blocking etc..
                    sockets.remove(sess.getRemoteAddress().toString());
                }
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        String host = getSession().getRemoteAddress().toString();
        System.out.println("Socket Closed: " + host + " [" + statusCode + "] " + reason);
        super.onWebSocketClose(statusCode, reason);
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
