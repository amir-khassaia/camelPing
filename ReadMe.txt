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

eg. To monitor Spring 'petclinic' sample:
CamelPing -url http://localhost:8080/petclinic -period 10000 -delay 0
[                          main] MainSupport                    INFO  Apache Camel 2.11.0 starting
[                          main] DefaultCamelContext            INFO  Apache Camel 2.11.0 (CamelContext: camel-1) is starting
[                          main] ManagementStrategyFactory      INFO  JMX enabled.
[                          main] DefaultTypeConverter           INFO  Loaded 181 type converters
[                          main] DefaultCamelContext            INFO  Route: route1 started and consuming from: Endpoint[timer://camelPing?delay=0&fixedRate=true&period=10000]
[                          main] ultManagementLifecycleStrategy INFO  Load performance statistics enabled.
[                          main] DefaultCamelContext            INFO  Total 1 routes, of which 1 is started.
[                          main] DefaultCamelContext            INFO  Apache Camel 2.11.0 (CamelContext: camel-1) started in 0.332 seconds
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 1/1 (100%)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 2/2 (100%)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> SUCCESS (200): 3/3 (100%)
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> FAILURE (No response): 1/4 (75%): org.apache.camel.component.http.HttpOperationFailedException: HTTP operation failed invoking http://localhost:8080/petclinic with statusCode: 503
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> FAILURE (No response): 2/5 (60%): java.net.ConnectException: Connection refused: connect
[ thread #0 - timer://camelPing] route1                         INFO  >>> Polling endpoint: http://localhost:8080/petclinic
[ thread #0 - timer://camelPing] MyRouteBuilder                 INFO  >>> FAILURE (No response): 3/6 (50%): java.net.ConnectException: Connection refused: connect
