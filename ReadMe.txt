Camel Java Router Project
=========================

To build this project use

    mvn install

To run this project from within Maven use

    mvn exec:java

For more help see the Apache Camel documentation

    http://camel.apache.org/


Illustrates a contrived example of using Camel route to poll an endpoint and post process 
the results in a custom processor exposed via JMX. 

Invoke as follows:
CamelPing -url <url to poll> -period <period of polling in ms> -delay <initial delay in ms>

eg. To monitor Spring 'petclinic' sample:
CamelPing -url http://localhost:8080/petclinic -period 10000 -delay 0
[                          main] MainSupport                    INFO  Apache Camel 2.11.0 starting
[                          main] DefaultCamelContext            INFO  Apache Camel 2.11.0 (CamelContext: camel-1) is starting
[                          main] ManagementStrategyFactory      INFO  JMX enabled.
[                          main] DefaultTypeConverter           INFO  Loaded 181 type converters
[                          main] DefaultCamelContext            INFO  Route: route1 started and consuming from: Endpoint[timer://camelPing?delay=0&fixedRate=true&period=10000]
[                          main] ultManagementLifecycleStrategy INFO  Load performance statistics enabled.
[                          main] DefaultCamelContext            INFO  Total 1 routes, of which 1 is started.
[                          main] DefaultCamelContext            INFO  Apache Camel 2.11.0 (CamelContext: camel-1) started in 0.346 seconds
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 1/1 succeeded 100% (last 1 succeeded)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 2/2 succeeded 100% (last 2 succeeded)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 3/3 succeeded 100% (last 3 succeeded)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> FAILURE (No response): 1/4 failed 75% (last 1 failed): java.net.ConnectException: Connection refused: connect
