package org.bitpimp.camelPing;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

	// Route args
	private String url;
	private int delay = 0;
	private int period = 10000;
	
	// Processor in charge of ping
	private final Processor processor = new PingProcessor();

	@ManagedResource(description="Ping statistics")
	public final class PingProcessor implements Processor {
		long succeeded = 0;
		long failed = 0;
		long total = 0;
		long percentageSuccess = 0;
		
		@ManagedAttribute
		public long getSucceeded() {
			return succeeded;
		}

		@ManagedAttribute
		public long getFailed() {
			return failed;
		}

		@ManagedAttribute
		public long getTotal() {
			return total;
		}

		@ManagedAttribute
		public long getPercentageSuccess() {
			return percentageSuccess;
		}

		/**
		 * Process exchange and determine the state of the operation
		 */
		@Override
		public void process(final Exchange exchange) throws Exception {
			// Examine for any exceptions in the exchange
			Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
			boolean isFailure = exception != null;

			// Tally up stats
			total++;
			if (isFailure) 
				failed++; 
			else 
				succeeded++;
			percentageSuccess =  (100 * succeeded) / total;
			
			// Look at HTTP response code (if present)
			Message out = exchange.getIn();
			Integer responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
			
			if (isFailure) {
				log.info(String.format(">>> FAILURE (%s): %d/%d (%d%%): %s", 
						responseCode != null ? responseCode : "No response", 
						failed, total, percentageSuccess, exception));
			} else {
				log.info(String.format(">>> SUCCESS (%s): %d/%d (%d%%)", 
						responseCode != null ? responseCode : "No response",
						succeeded, total, percentageSuccess));
			}
		}
	}

	public MyRouteBuilder() {
		url = "http://localhost:8080/petclinic/";
	}
	
    public MyRouteBuilder(String[] args) {
    	for (int i = 0; i < args.length; i++) {
    		if ("-url".equals(args[i]))
    			url = args[++i];
    		else if ("-delay".equals(args[i]))
    			delay = Integer.parseInt(args[++i]);
    		else if ("-period".equals(args[i]))
    			period = Integer.parseInt(args[++i]);
    	}
	}

	/**
     * Let's configure the Camel routing rules using Java code...
     */
    @Override
    public void configure() {
    	// Configure Apache HTTP commons transport component to not retry on failure 
    	// (we just want a single success/failure for the purposes of this exercise)
    	HttpComponent httpComponent = getContext().getComponent("http", HttpComponent.class);
    	httpComponent.setHttpClientConfigurer(new HttpClientConfigurer() {
    		@Override
    		public void configureHttpClient(HttpClient client) {
    			// Interpose non retrying handling
    			DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(0, false); 
    			client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler); 
    		}
    	});

    	// only camel-http for now
    	if (!url.startsWith("http://"))
    		throw new IllegalArgumentException("Camel-HTTP endpoints only!");

    	// Build the timed/polling route interposing a custom processor instance to deal with endpoint status 
    	from(String.format("timer://camelPing?fixedRate=true&delay=%d&period=%d", delay, period))
			.doTry()
				.log(">>> Polling endpoint: " + url)
				.to(url + "?throwExceptionOnFailure=true")
			.doCatch(Exception.class)
			.doFinally()
				.process(processor);
    }
}
