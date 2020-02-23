package com.serverless;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterInstance {

    private static Twitter INSTANCE;

    /**
     * I am using a singleton here because Lambda reuses the program between runs.
     * Also, the Twitter library crashes if you attempt to initialize it multiple times.
     * This should fix that issue.
     */
    public static synchronized Twitter getTwitterInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(System.getenv(Ids.TWITTER_API_KEY));
        builder.setOAuthConsumerSecret(System.getenv(Ids.TWITTER_API_SECRET_KEY));
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        Twitter twitter = factory.getInstance();
        twitter.setOAuthAccessToken(new AccessToken(System.getenv(Ids.TWITTER_ACCESS_TOKEN), System.getenv(Ids.TWITTER_ACCESS_TOKEN_SECRET)));

        INSTANCE = twitter;
        return INSTANCE;
    }

}
