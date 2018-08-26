package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, Void> {

	private static final Logger LOG = LogManager.getLogger(Handler.class);

	@Override
	public Void handleRequest(Map<String, Object> input, Context context) {
		LOG.debug("Starting program");

		try {
			new TaglineManager().run();
		} catch (Exception e) {
			LOG.error(e);
			LOG.error(e.getStackTrace());
			throw new RuntimeException(e);
		}

		LOG.debug("Finished program");
		return null;
	}
}
