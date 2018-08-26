package com.serverless;

import com.serverless.model.Tagline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import twitter4j.*;
import twitter4j.api.TweetsResources;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.List;

import static com.serverless.Utils.randomLetter;
import static com.serverless.Utils.urlToInputStream;


public class TaglineManager {

    private static final Logger LOG = LogManager.getLogger(Handler.class);

    public void run() throws Exception {
        Tagline tagline = getCurrentTagline();

        LOG.debug("Current tagline: " + tagline);

        if (isNewTagline(tagline)) {
            LOG.debug("New tagline");
            addTaglineToDatabase(tagline);
            LOG.debug("Added to database");
            Status status = postToTwitter(tagline);
            LOG.debug("Tweeted " + status);
        } else {
            LOG.debug("Old tagline");
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
        String accessKey = Ids.SCREENSHOT_LAYER_API_KEY;
        // Screenshot layer and twitter are caching images. This is used to regenerate the screenshot
        String randomParam = "" + randomLetter() + randomLetter() + randomLetter();
        String encodedUrl = "https%3A%2F%2Fwww.theverge.com";
        String cssUrl = "https://gist.githubusercontent.com/TylerCarberry/50ebef6ac7b67c7cf8fa6371a8724c18/raw/800fe13e01b778607e8943e4886b73fbebd58907/adblock.css";

        return "http://api.screenshotlayer.com/api/capture?access_key=" + accessKey + "&url=" + encodedUrl + "&viewport=1024x280&width=1024&force=1&ttl=2000&delay=5&css_url=" + cssUrl;
    }

    private List<Tagline> getAllFromDatabase() {
        Sql2o sql2o = new Sql2o("jdbc:mysql://" + Ids.DATABASE_HOST + ":3306/" + Ids.DATABASE_DB, Ids.DATABASE_USER, Ids.DATABASE_PASSWORD);

        String sql = "SELECT text, href, background FROM taglines";

        try (Connection con = sql2o.open()) {
            return con.createQuery(sql).executeAndFetch(Tagline.class);
        }

    }

    private void addTaglineToDatabase(Tagline tagline) {
        Sql2o sql2o = new Sql2o("jdbc:mysql://" + Ids.DATABASE_HOST + ":3306/" + Ids.DATABASE_DB, Ids.DATABASE_USER, Ids.DATABASE_PASSWORD);

        final String insertQuery = "INSERT INTO taglines (text, href, background)  VALUES (:text, :href, :background)";

        try (Connection con = sql2o.beginTransaction()) {
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

        LOG.debug("Uploading screenshot");
        UploadedMedia screenshot = tweetsResources.uploadMedia("Screenshot.png", urlToInputStream(getScreenshotUrl()));
        LOG.debug("Uploading background");
        UploadedMedia background = tweetsResources.uploadMedia("Background.png", urlToInputStream(tagline.getBackground()));
        LOG.debug("Finished uploaded media");

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

}
