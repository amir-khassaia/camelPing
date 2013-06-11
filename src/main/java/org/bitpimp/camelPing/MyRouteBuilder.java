package org.bitpimp.camelPing;

import org.apache.camel.Endpoint;
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
	private final PingProcessor pingProcessor = new PingProcessor();

	// Alert handling processor for first up/down event
	private final Processor alertHandler;

	// The endpoint being polled
	private Endpoint endpoint ;


	/**
	 * Demonstrates a custom POJO bean that is invoked whenever the endpoint goes
	 * offline or online for the first time
	 *
	 */
	public class AlertHandler implements Processor{
		@Handler
		@Override
		public void process(Exchange exchange) throws Exception {
			final Object exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
			final String endpoint = (String) exchange.getProperty(Exchange.TO_ENDPOINT);
			log.info(String.format(">>> %s ALERT: %s",
					exception != null ? "OFFLINE" : "ONLINE", endpoint));
		}
	}

	/**
	 * A Camel processor that handles endpoint ping results
	 * and exports them over JMX
	 *
	 */
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

		public synchronized void reset() {
			this.succeeded = 0;
			this.total = 0;
			this.failed = 0;
			this.lastFailed = 0;
			this.lastSucceeded = 0;
			this.offlineSince = 0;
			this.onlineSince = 0;
			this.percentageSuccess = 0;
			this.percentageSuccess = 0;
		}
	}

    public MyRouteBuilder(final String[] args, final Processor alertHandler) {
    	if (args != null)
	    	for (int i = 0; i < args.length; i++) {
	    		if ("-url".equals(args[i]))
	    			url = args[++i];
	    		else if ("-delay".equals(args[i]))
	    			delay = Integer.parseInt(args[++i]);
	    		else if ("-period".equals(args[i]))
	    			period = Integer.parseInt(args[++i]);
	    	}
    	this.alertHandler = alertHandler == null ? new AlertHandler() : alertHandler;
	}

	public MyRouteBuilder(final Processor alertHandler) {
		this(null, alertHandler);
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

    	if (url != null)
    		// Obtain endpoint from URI
    		endpoint = getContext().getEndpoint(url);
    	else if (endpoint == null)
    		throw new IllegalArgumentException("No endpoint instance or URI set");

    	// Build the timed/polling route interposing a custom processor instance to deal with endpoint status
    	from(String.format("timer://camelPing?fixedRate=true&delay=%d&period=%d", delay, period))
			.doTry()
				.log(">>> Polling endpoint: " + endpoint.getEndpointUri())
				.to(endpoint)
			.doCatch(Exception.class)
			.doFinally()
				.process(pingProcessor)
				// Filter to messages marked for alert handling and send to alert handling bean
				.filter(header(PingProcessor.MESSAGE_ALERT).isNotNull())
				.bean(alertHandler);
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public PingProcessor getPingProcessor() {
		return pingProcessor;
	}
}
