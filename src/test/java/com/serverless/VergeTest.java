package com.serverless;

import com.serverless.TwitterInstance;
import org.junit.jupiter.api.Test;
import twitter4j.Twitter;

public class VergeTest {

    /**
     * If this test fails then something is improperly configured on CircleCI
     */
    public void testAlwaysPasses() {
        assert true;
    }

    @Test
    public void testTwitterConnection() {
        // Getting the instance multiple time shouldn't break anything
        Twitter twitterInstance = TwitterInstance.getTwitterInstance();
        Twitter twitterInstance2 = TwitterInstance.getTwitterInstance();

        assert twitterInstance == twitterInstance2;
    }

}
