package com.example.reactiveBTC;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * The WebServer
 *
 * 1. Creates an embedded Jetty server.
 *    Registers the TradeServlet class for handling the trades web sockets.
 *    Adds a simple file handler
 * 2. Starts the Cryptsy data loading process
 * 3. Starts the TailTrades tailing of the trades capped collection.
 * 4. Opens the browser at http://localhost:8080.
 *
 * Only caters for the happy path, error cases etc are ignored as this is a demo ;)
 */
public class WebServer
{
    public static void main(String[] args)
    {

        Thread webServer = new Thread(() -> {
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(8080);
            server.addConnector(connector);

            // Setup the basic application "context" for this application at "/"
            // This is also known as the handler tree (in jetty speak)
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            ServletHolder holderEvents = new ServletHolder("ws-events", TradeServlet.class);
            context.addServlet(holderEvents, "/trades/*");

            // Add handler for the static files
            ResourceHandler resource_handler = new ResourceHandler();
            resource_handler.setDirectoriesListed(false);
            resource_handler.setWelcomeFiles(new String[]{"index.html"});
            resource_handler.setResourceBase("./web");

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{resource_handler, context, new DefaultHandler()});

            server.setHandler(handlers);

            try {
                server.start();
                server.dump(System.err);
                server.join();
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        });


        boolean simulate = System.getProperties().containsKey("simulate");
        String uri = System.getProperty("uri", "mongodb://localhost:27017");

        System.out.println("Starting Trades");
        Thread tailTrades =  new Thread(() -> {
            try {
                new TailTrades(uri);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        });
        tailTrades.setName("Tailing Trades Thread");

        System.out.println("Starting Cryptsy");
        Thread cryptsy =  new Thread(() -> {
            try {
                new Cryptsy(simulate, uri);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        });
        cryptsy.setName("Cryptsy Thread");



        try {
            webServer.start();
            tailTrades.start();

            Thread.sleep(1000);
            cryptsy.start();

            Thread.sleep(1000);
            Desktop.getDesktop().browse(URI.create("http://localhost:8080/"));

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
