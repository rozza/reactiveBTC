reactiveBTC
===========

A demo reactive application using the Async MongoDB Java driver

![Screen shot](https://github.com/rozza/reactiveBTC/raw/master/screenshot.png)

Simply run:

    ./gradlew webServer

It should download all requirements and open the browser to: http://localhost:8080

When the refresh icon spins new data has been sent from the capped collection to the browser via a websocket.


To simulate the data (eg. you're offline) use:

    ./gradlew -Dsimulate="true" webServer

\* You will need your maven installed packages locally!
    
To change the mongodb location use `-Duri="mongodb://Mongo_URI"`

How it works
============

The `Cryptsy` task / service gets various bit coin prices and inserts them into a capped collection in mongodb.
The `TailTrades` task / service reads data from the capped collection and then notifies the `TradeSocket` which 
sends the trade document to all open websockets. In the browser when we get new data we add that tick to the d3 
graph dataset and spin the refresh icon.

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

