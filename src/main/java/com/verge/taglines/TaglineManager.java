package com.verge.taglines;

import com.verge.taglines.model.EnvironmentVariable;
import com.verge.taglines.model.Tagline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import twitter4j.*;
import twitter4j.api.TweetsResources;

import java.io.IOException;
import java.util.List;
import java.util.logging.Handler;

import static com.verge.taglines.Utils.randomLetter;
import static com.verge.taglines.Utils.urlToInputStream;


public class TaglineManager {

    private static final Logger LOG = LogManager.getLogger(Handler.class);
    private DiscordLogger discordLogger = new DiscordLogger();

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

    private boolean isNewTagline(Tagline tagline) {
        List<Tagline> allFromDatabase = getAllFromDatabase();
        return !allFromDatabase.contains(tagline);
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

        Element taglineElement = homepage.select(".c-masthead__tagline").get(0);
        String href = null;

        try {
            href = taglineElement.getAllElements().get(1).attr("href");
        } catch (Exception e) {
            // No href
        }
        String taglineText = taglineElement.text();

        Element backgroundElement = homepage.select(".c-masthead__main").get(0);
        String style = backgroundElement.attr("style");
        String background = style.substring("background-image:url(".length(), style.length() - 1);

        return new Tagline(taglineText, href, background);
    }

    private String getScreenshotUrl() {
        EnvironmentVariable screenshotLayerApiKey = Math.random() < 0.5 ? EnvironmentVariable.SCREENSHOT_LAYER_API_KEY : EnvironmentVariable.SCREENSHOT_LAYER_FALLBACK_API_KEY;
        // Screenshot layer and twitter are caching images. This is used to regenerate the screenshot
        String randomParam = "" + randomLetter() + randomLetter() + randomLetter();
        String encodedUrl = "https%3A%2F%2Fwww.theverge.com";
        String cssUrl = "https://gist.githubusercontent.com/TylerCarberry/50ebef6ac7b67c7cf8fa6371a8724c18/raw/800fe13e01b778607e8943e4886b73fbebd58907/adblock.css";

        return "http://api.screenshotlayer.com/api/capture?access_key=" + screenshotLayerApiKey.getValue() + "&url=" + encodedUrl + "&viewport=1200x300&width=1024&force=1&ttl=2000&delay=5&css_url=" + cssUrl;
    }

    private List<Tagline> getAllFromDatabase() {
        String sql = "SELECT text, href, background FROM taglines ORDER BY timestamp DESC LIMIT 100";

        try (Connection con = getDatabaseConnection().open()) {
            return con.createQuery(sql).executeAndFetch(Tagline.class);
        }

    }

    private void addTaglineToDatabase(Tagline tagline) {
        final String insertQuery = "INSERT INTO taglines (text, href, background)  VALUES (:text, :href, :background)";

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
        UploadedMedia screenshot = tweetsResources.uploadMedia("Screenshot.png", urlToInputStream(getScreenshotUrl()));
        log("Uploading background");
        UploadedMedia background = tweetsResources.uploadMedia("Background.png", urlToInputStream(tagline.getBackground()));
        log("Finished uploaded media");

        String whatToPost = tagline.getText();
        if (tagline.getHref() != null && tagline.getHref().length() > 5) {
            whatToPost += " " + tagline.getHref();
        }

        StatusUpdate statusUpdate = new StatusUpdate(whatToPost);

        // Sometimes the header background is a gif
        if (tagline.getBackground().toLowerCase().endsWith("gif")) {
            // Twitter doesn't allow you to post both a gif (the background) and
            // a picture (the screenshot). If this is the case, just post the pic background.
            statusUpdate.setMediaIds(background.getMediaId());
        } else {
            statusUpdate.setMediaIds(screenshot.getMediaId(), background.getMediaId());
        }

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
