ReactiveBTC
===========

A demo reactive application using Java 8, the Async MongoDB Java driver and websockets.

![Screen shot](https://github.com/rozza/reactiveBTC/raw/master/screenshot.png)

Simply run:

    ./gradlew webServer

It should download all requirements and open the browser to: http://localhost:8080

When the refresh icon spins new data has been sent from the capped collection to the browser via a websocket.


To simulate the data (eg. you're offline) use:

    ./gradlew -Dsimulate webServer

\* You will need your maven installed packages locally!
    
To change the mongodb location use `-Duri="mongodb://Mongo_URI"`

How it works
============

The `Cryptsy` task / service gets various bit coin prices and inserts them into a capped collection in mongodb.
The `TailTrades` task / service reads data from the capped collection and then notifies the `TradeSocket` which 
sends the trade document to all open websockets. In the browser the graph continuously refreshes over time but we get 
new data via the websocket we add that to the graph dataset and spin the refresh icon. 

         +-----------------------+                      
         |                       |                      
         |        Browser        |                      
         |                       |                      
         +-+---------------+-----+                      
           ^               |  ^                          
           |               V  |                          
           |          +-------+---+                      
           |          |   Jetty   |                      
           |          +-----------+                      
           |                            
           V                            
     +-----------+                      
     |TradeSocket|                      
     +-----------+                      
           ^                            
           |                            
    +------+------+       +------------+
    |             |       |            |
    | TailTrades  |       |  Cryptsy   |
    |             |       |            |
    +-------------+       +-----+------+
           ^                    |      
           |                    |      
           |   +-----------+    |      
           |   |           |    |      
           +-+ |  MongoDB  | <--+      
               |           |           
               +-----------+                

