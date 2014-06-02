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


        System.out.println("Starting Cryptsy");
        Thread cryptsy =  new Thread(() -> Cryptsy.main(args));
        cryptsy.setName("Cryptsy Thread");


        System.out.println("Starting Trades");
        Thread tailTrades =  new Thread(() -> TailTrades.main(args));
        tailTrades.setName("Tailing Trades Thread");

        try {
            webServer.start();
            cryptsy.start();
            tailTrades.start();

            Thread.sleep(1000);
            Desktop.getDesktop().browse(URI.create("http://localhost:8080/"));

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
