package com.verge.taglines;

import com.verge.taglines.model.EnvironmentVariable;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * I am using a singleton here because Lambda reuses the program between runs.
 * Also, the Twitter library crashes if you attempt to initialize it multiple times.
 * This should fix that issue.
 */
public class TwitterInstance {

    private static Twitter INSTANCE;

    public static synchronized Twitter getTwitterInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(EnvironmentVariable.TWITTER_API_KEY.getValue());
        builder.setOAuthConsumerSecret(EnvironmentVariable.TWITTER_API_SECRET_KEY.getValue());
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        Twitter twitter = factory.getInstance();
        twitter.setOAuthAccessToken(new AccessToken(
                EnvironmentVariable.TWITTER_ACCESS_TOKEN.getValue(),
                EnvironmentVariable.TWITTER_ACCESS_TOKEN_SECRET.getValue())
        );

        INSTANCE = twitter;
        return INSTANCE;
    }

}
