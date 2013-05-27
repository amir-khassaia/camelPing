package org.bitpimp.camelPing;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
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

	/**
	 * Demonstrates a custom POJO bean that is invoked whenever the endpoint goes offline or online for the first time
	 * 
	 */
	public final class AlertHandler {
		@Handler
		public void alertHandler(final Exchange exchange) {
			final Object exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
			final String endpoint = (String) exchange.getProperty(Exchange.TO_ENDPOINT);
			log.info(String.format(">>> %s ALERT: %s", exception != null ? "OFFLINE" : "ONLINE", endpoint));
		}
	}

	@ManagedResource(description="Ping statistics")
	public final class PingProcessor implements Processor {
		private static final String MESSAGE_ALERT = "MESSAGE_ALERT";
		long succeeded = 0;
		long failed = 0;
		long total = 0;
		long percentageSuccess = 0;
		
		long lastSucceeded = 0;
		long lastFailed = 0;
		
		long onlineSince = 0;
		long offlineSince = 0;
		
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
		
		@ManagedAttribute
		public long getLastSucceeded() {
			return lastSucceeded;
		}

		@ManagedAttribute
		public long getLastFailed() {
			return lastFailed;
		}
		
		@ManagedAttribute
		public long getOnlineSince() {
			return onlineSince;
		}

		@ManagedAttribute
		public long getOfflineSince() {
			return offlineSince;
		}

		/**
		 * Process exchange and determine the state of the operation
		 */
		@Override
		public synchronized void process(final Exchange exchange) throws Exception {
			Message in = exchange.getIn();
			// Examine for any exceptions in the exchange
			Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
			boolean isFailure = exception != null;

			// Tally up stats
			total++;
			if (isFailure) {
				failed++;
				lastFailed++;
				if (lastSucceeded > 0)
					lastSucceeded = 0;

				if (lastFailed == 1) {
					in.setHeader(MESSAGE_ALERT, true); // first failure in sequence
					offlineSince = System.currentTimeMillis();
				}
			}
			else {
				succeeded++;
				lastSucceeded++;
				if (lastFailed > 0)
					lastFailed = 0;
				if (lastSucceeded == 1) {
					in.setHeader(MESSAGE_ALERT, true); // first success in sequence
					onlineSince = System.currentTimeMillis();
				}
			}
			percentageSuccess =  (100 * succeeded) / total;
			
			// Look at HTTP response code (if present)
			Integer responseCode = in.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
			
			if (isFailure) {
				log.info(String.format(">>> FAILURE (%s): %d/%d failed %d%% (last %d failed, offline=%s): %s", 
						responseCode != null ? responseCode : "No response", 
						failed, total, percentageSuccess, lastFailed, 
						toDuration(System.currentTimeMillis() - offlineSince), exception));
			} else {
				log.info(String.format(">>> SUCCESS (%s): %d/%d succeeded %d%% (last %d succeeded, online=%s)", 
						responseCode != null ? responseCode : "No response",
						succeeded, total, percentageSuccess, lastSucceeded, 
						toDuration(System.currentTimeMillis() - onlineSince)));
			}
		}
		
		private String toDuration(long duration) {
			duration /= 1000;
			return String.format("%dh:%02dm:%02ds", duration/3600, (duration%3600)/60, (duration%60));
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
				.process(processor)
				// Filter to messages marked for alert handling and send to alert handling bean
				.filter(header(PingProcessor.MESSAGE_ALERT).isNotNull())
				.bean(new AlertHandler());
    }
}
