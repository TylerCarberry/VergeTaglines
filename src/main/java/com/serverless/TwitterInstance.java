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
        builder.setOAuthConsumerKey(Ids.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(Ids.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        Twitter twitter = factory.getInstance();
        twitter.setOAuthAccessToken(new AccessToken(Ids.TWITTER_ACCESS_TOKEN_KEY, Ids.TWITTER_ACCESS_TOKEN_SECRET));

        INSTANCE = twitter;
        return INSTANCE;
    }

}
