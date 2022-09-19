package com.verge.taglines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verge.taglines.model.EnvironmentVariable;
import com.verge.taglines.model.ScreenshotResponse;
import com.verge.taglines.model.Tagline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.lang.NonNull;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import twitter4j.*;
import twitter4j.api.TweetsResources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Handler;


public class TaglineManager {

    private static final Logger LOG = LogManager.getLogger(Handler.class);
    private final DiscordLogger discordLogger = new DiscordLogger();

    public String run() throws Exception {
        Tagline tagline = getCurrentTagline();
        LOG.info(String.format("Current tagline: %s", tagline));

        if (isNewTagline(tagline)) {
            log("New tagline");
            log(String.format("`%s`", tagline));
            addTaglineToDatabase(tagline);
            log("Added to database");
            Status status = postToTwitter(tagline);
            log(String.format("Tweeted `%s`", status));
            String tweetUrl = "https://twitter.com/VergeTaglines/status/" + status.getId();
            new DiscordLogger().postOnDiscord(tagline, tweetUrl);
            return tweetUrl;
        } else {
            LOG.info("Old tagline");
            return "Old tagline";
        }
    }

    /**
     * @return the current tagline from the homepage of theverge.com
     * @throws IOException If the bot is unable to connect to the website
     */
    private Tagline getCurrentTagline() throws IOException {
        Document homepage = Jsoup.connect("https://www.theverge.com")
                // Add a header to ensure The Verge doesn't block the request
                .header("User-Agent", "@VergeTaglines")
                .get();

        Element taglineElement = homepage.select(".duet--recirculation--storystream-header").select("a").get(0);
        String href = null;

        try {
            href = taglineElement.attr("href");
        } catch (Exception e) {
            // No href
        }
        String taglineText = taglineElement.text();
        return new Tagline(taglineText, href, null);
    }

    private InputStream getScreenshot(@NonNull String href) throws IOException {
        EnvironmentVariable screenshotApiKey = Math.random() < 0.5 ? EnvironmentVariable.SCREENSHOT_API_KEY : EnvironmentVariable.SCREENSHOT_FALLBACK_API_KEY;
        String encoded = URLEncoder.encode(href, StandardCharsets.UTF_8.toString());

        String iphoneUserAgent = "Mozilla%2F5.0%20%28iPhone%3B%20CPU%20iPhone%20OS%2012_2%20like%20Mac%20OS%20X%29%20AppleWebKit%2F605.1.15%20%28KHTML%2C%20like%20Gecko%29%20Mobile%2F15E148";
        String url = "https://api.apiflash.com/v1/urltoimage?" +
                "access_key=" + screenshotApiKey +
                "&url=" + encoded +
                "&format=png" +
                "&width=720" +
                "&height=720" +
                "&fresh=true" +
                "&user_agent=" + iphoneUserAgent +
                "&delay=10" +
                "&response_type=json";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("accept", "application/json");

        InputStream responseStream = connection.getInputStream();
        ScreenshotResponse response = new ObjectMapper().readValue(responseStream, ScreenshotResponse.class);
        BufferedImage image = ImageIO.read(new URL(response.url));
        return Utils.pngToInputStream(image);
    }

    private boolean isNewTagline(Tagline tagline) {
        String sql = "SELECT text, href FROM taglines " +
                "WHERE href = :href AND text = :text";

        try (Connection con = getDatabaseConnection().open()) {
            List<Tagline> matchingTaglines = con.createQuery(sql)
                    .addParameter("href", tagline.getHref())
                    .addParameter("text", tagline.getText())
                    .executeAndFetch(Tagline.class);
            return matchingTaglines.isEmpty();
        }
    }

    private void addTaglineToDatabase(Tagline tagline) {
        final String insertQuery = "INSERT INTO taglines (text, href, background) VALUES (:text, :href, :background)";

        try (Connection con = getDatabaseConnection().beginTransaction()) {
            con.createQuery(insertQuery)
                    .addParameter("text", tagline.getText())
                    .addParameter("href", tagline.getHref())
                    .addParameter("background", tagline.getBackground())
                    .executeUpdate();
            con.commit();
        }
    }

    private Status postToTwitter(Tagline tagline) throws IOException, TwitterException {
        Twitter twitter = TwitterInstance.getTwitterInstance();

        TweetsResources tweetsResources = twitter.tweets();

        log("Uploading screenshot");
        UploadedMedia screenshot = tweetsResources.uploadMedia("Screenshot.png", getScreenshot(tagline.getHref()));
        log("Finished uploaded media");

        String whatToPost = tagline.getText();
        if (tagline.getHref() != null && tagline.getHref().length() > 5) {
            whatToPost += " " + tagline.getHref();
        }

        StatusUpdate statusUpdate = new StatusUpdate(whatToPost);
        statusUpdate.setMediaIds(screenshot.getMediaId());

        return twitter.updateStatus(statusUpdate);
    }

    private Sql2o getDatabaseConnection() {
        return new Sql2o(String.format("jdbc:mysql://%s:3306/%s?useUnicode=yes&characterEncoding=UTF-8",
                EnvironmentVariable.DATABASE_HOST.getValue(),
                EnvironmentVariable.DATABASE_SCHEMA.getValue()),
                EnvironmentVariable.DATABASE_USER.getValue(),
                EnvironmentVariable.DATABASE_PASSWORD.getValue()
        );
    }

    private void log(String message) {
        LOG.info(message);
        try {
            discordLogger.logToDiscord(message);
        } catch (IOException e) {
            LOG.error("Unable to post to discord", e);
        }
    }

}
