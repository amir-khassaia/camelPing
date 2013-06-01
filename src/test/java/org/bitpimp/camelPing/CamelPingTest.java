package org.bitpimp.camelPing;

import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test Camel route using online and offline scenarios
 *  
 */
public class CamelPingTest extends CamelTestSupport {

	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	private final MyRouteBuilder builder;
	private int alerts = 0;
	
	public CamelPingTest() {
		builder = new MyRouteBuilder(new Processor() {
			@Override
			public synchronized void process(Exchange exchange) throws Exception {
				// Alert handler
				alerts++;
				log.info("ALERT HANDLER-" + alerts + ": " + exchange.getIn().getMessageId());
			}
		});
	}

	@Before
	public void init() {
		// Reset values of interest prior to each test
		alerts = 0;
		builder.getPingProcessor().reset();
	}	
	
	@Test
	public void testOnline() throws InterruptedException {
		// Wait for 5 seconds while Camel route is exercised
		resultEndpoint.whenAnyExchangeReceived(null);
		Thread.sleep(5000);
		// Ensure online symptoms are reported
		List<Exchange> exchanges = resultEndpoint.getExchanges();
		Assert.assertTrue(!exchanges.isEmpty());
		Assert.assertEquals(1, alerts);
		Assert.assertEquals(exchanges.size(), builder.getPingProcessor().getSucceeded());
		Assert.assertEquals(0, builder.getPingProcessor().getFailed());
		Assert.assertEquals(exchanges.size(), builder.getPingProcessor().getTotal());
	}

	@Test
	public void testOffline() throws InterruptedException {
		resultEndpoint.whenAnyExchangeReceived(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				// Treat the endpoint as offline
				throw new Exception("OFFLINE");
			}
		});
		
		// Wait for 5 seconds while Camel route is exercised
		Thread.sleep(5000);
		// Ensure offline symptoms are reported
		final List<Exchange> exchanges = resultEndpoint.getExchanges();
		Assert.assertTrue(!exchanges.isEmpty());
		Assert.assertEquals(1, alerts);
		Assert.assertEquals(exchanges.size(), builder.getPingProcessor().getFailed());
		Assert.assertEquals(0, builder.getPingProcessor().getSucceeded());
		Assert.assertEquals(exchanges.size(), builder.getPingProcessor().getTotal());
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		builder.setDelay(0);
		builder.setPeriod(1000);
		builder.setEndpoint(resultEndpoint);
		return builder;
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		resultEndpoint.setMinimumExpectedMessageCount(1);
		assertMockEndpointsSatisfied();
	}
}
