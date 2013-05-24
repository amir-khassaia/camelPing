Camel Java Router Project
=========================

To build this project use

    mvn install

To run this project from within Maven use

    mvn exec:java

For more help see the Apache Camel documentation

    http://camel.apache.org/


Illustrates a contrived example of using Camel route to poll an endpoint and post process the results in a custom processor exposed via JMX.

Invoke with CamelPing -url <url to poll> -period <period of polling in ms> -delay <initial delay in ms>