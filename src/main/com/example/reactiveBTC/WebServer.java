package com.example.reactiveBTC;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerContainer;

public class WebServer
{
    public static void main(String[] args)
    {


        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add handler for the static files
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(false);
        resource_handler.setWelcomeFiles(new String[]{"index.html"});

        resource_handler.setResourceBase("./web");
        server.setHandler(resource_handler);

        try {
            // Initialize javax.websocket layer
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(TradeSocket.class);

            server.start();
            server.dump(System.err);
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }

//        System.out.println("Starting Cryptsy");
//        Thread cryptsy =  new Thread(() -> Cryptsy.main(args));
//        cryptsy.setName("Cryptsy Thread");
//        cryptsy.start();

        System.out.println("Starting Trades");
//        Thread tailTrades =  new Thread(() -> TailTrades.main(args));
//        tailTrades.setName("Tailing Trades Thread");
//        tailTrades.start();

    }
}
