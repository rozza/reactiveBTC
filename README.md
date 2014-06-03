reactiveBTC
===========

A demo reactive application using the Async MongoDB Java driver

Simply run:

    ./gradlew webServer

It should download all requirements and open the browser to: http://localhost:8080

When the refresh icon spins new data has been sent from the capped collection to the browser.


To simulate the data (eg. you're offline) use:

    ./gradlew -Dsimulate="true" webServer

* You will need your maven installed packages locally!
    
To change the mongodb location use `-Duri="mongodb://Mongo_URI"`
