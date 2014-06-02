package com.example.reactiveBTC;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class TradeSocket extends WebSocketAdapter
{

    private final static ConcurrentHashMap<InetSocketAddress, Session> sockets = new ConcurrentHashMap<>();

    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        sockets.put(sess.getLocalAddress(), sess);
        System.out.println("Socket Connected: " + sess);
    }

    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        for (Session sess: sockets.values()){
            if (!sess.isOpen()) {
                sockets.remove(sess.getLocalAddress());
            } else {
                try {
                    System.out.println(sess.getUpgradeRequest());
                    sess.getRemote().sendString(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
