package com.verge.taglines;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verge.taglines.model.EnvironmentVariable;
import com.verge.taglines.model.ScreenshotResponse;
import com.verge.taglines.model.Tagline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import twitter4j.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;


public class TaglineManager {

    private static final Logger LOG = LogManager.getLogger(Handler.class);
    private final DiscordLogger discordLogger = new DiscordLogger();

    public String run() throws Exception {
        Tagline tagline = null;

        Tagline elonJetStatus = getElonJetStatus();
        if (elonJetStatus != null && isNewElonTagline(elonJetStatus)) {
            tagline = elonJetStatus;
        } else {
            Tagline vergeTagline = getCurrentTagline();
            LOG.info(String.format("Current tagline: %s", vergeTagline));
            if (isNewVergeTagline(vergeTagline)) {
                tagline = vergeTagline;
            }
        }

        if (tagline != null) {
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

        String nextData = homepage.getElementById("__NEXT_DATA__").html();


        ObjectMapper mapper = new ObjectMapper();

        // hacky parsing
        Map<String, Object> map = mapper.readValue(nextData, new TypeReference<HashMap>() {});
        Map props = (Map) map.get("props");
        Map pageProps = (Map) props.get("pageProps");
        Map hydration = (Map) pageProps.get("hydration");
        List responses = (List) hydration.get("responses");
        Map data = (Map) ((Map) responses.get(0)).get("data");
        Map cellData = (Map) data.get("cellData");
        Map prestoComponentData = (Map) cellData.get("prestoComponentData");

        String tagline = (String) prestoComponentData.getOrDefault("masthead_tagline", null);
        String url = (String) prestoComponentData.getOrDefault("masthead_tagline_url", null);

        return new Tagline(tagline, url, null);
    }

    @Nullable
    private Tagline getElonJetStatus() {
        String verifiedImage = "https://user-images.githubusercontent.com/6628497/200092588-ce4b4679-90f2-4e60-9752-66302cd4cf21.png";
        String bannedImage = "https://user-images.githubusercontent.com/6628497/200090687-b0ab14a0-13c2-412c-afaa-369e66360aed.png";

        try {
            Twitter twitterInstance = TwitterInstance.getTwitterInstance();
            User elonJet = twitterInstance.showUser("elonjet");
            if (elonJet.isVerified()) {
                return new Tagline("@elonjet is now verified on Twitter", null, verifiedImage);
            }
        } catch (TwitterException e) {
            log("Twitter exception " + e);
            boolean hasUserBeenSuspended = e.getMessage().toLowerCase().contains("user has been suspended");
            if (hasUserBeenSuspended) {
                return new Tagline("Elon Musk has suspended @elonjet's Twitter account (again)", null, bannedImage);
            }
        }
        return null;
    }

    private InputStream getScreenshot(@NonNull String href) throws IOException {
        EnvironmentVariable screenshotApiKey = Math.random() < 0.5 ? EnvironmentVariable.SCREENSHOT_API_KEY : EnvironmentVariable.SCREENSHOT_FALLBACK_API_KEY;
        String encoded = URLEncoder.encode(href, StandardCharsets.UTF_8.toString());

        String iphoneUserAgent = "Mozilla%2F5.0%20%28iPhone%3B%20CPU%20iPhone%20OS%2012_2%20like%20Mac%20OS%20X%29%20AppleWebKit%2F605.1.15%20%28KHTML%2C%20like%20Gecko%29%20Mobile%2F15E148";
        String url = "https://api.apiflash.com/v1/urltoimage?" +
                "access_key=" + screenshotApiKey.getValue() +
                "&url=" + encoded +
                "&format=png" +
                "&width=720" +
                "&height=720" +
                "&fresh=true" +
                //"&user_agent=" + iphoneUserAgent +
                //"&delay=10" +
                "&response_type=json";

        log(url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("accept", "application/json");

        InputStream responseStream = connection.getInputStream();
        ScreenshotResponse response = new ObjectMapper().readValue(responseStream, ScreenshotResponse.class);
        log("Finished taking screenshot. " + response.url);

        BufferedImage image = ImageIO.read(new URL(response.url));
        return Utils.pngToInputStream(image);
    }

    private boolean isNewVergeTagline(Tagline tagline) {
        String sql = "SELECT text, href, background FROM taglines " +
                "WHERE href = :href AND text = :text";

        try (Connection con = getDatabaseConnection().open()) {
            List<Tagline> matchingTaglines = con.createQuery(sql)
                    .addParameter("href", tagline.getHref())
                    .addParameter("text", tagline.getText())
                    .executeAndFetch(Tagline.class);
            return matchingTaglines.isEmpty();
        }
    }

    private boolean isNewElonTagline(Tagline tagline) {
        String sql = "SELECT text, href, background FROM taglines " +
                "WHERE text = :text AND background = :background";

        try (Connection con = getDatabaseConnection().open()) {
            List<Tagline> matchingTaglines = con.createQuery(sql)
                    .addParameter("text", tagline.getText())
                    .addParameter("background", tagline.getBackground())
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

        String whatToPost = tagline.getText();
        if (tagline.getHref() != null && tagline.getHref().length() > 5) {
            whatToPost += " " + tagline.getHref();
        }

        StatusUpdate statusUpdate = new StatusUpdate(whatToPost);
        List<UploadedMedia> uploadedMediaList = uploadMedia(tagline);
        if (!uploadedMediaList.isEmpty()) {
            long[] mediaId = uploadedMediaList.stream().mapToLong(UploadedMedia::getMediaId).toArray();
            statusUpdate.setMediaIds(mediaId);
        }

        return twitter.updateStatus(statusUpdate);
    }

    private List<UploadedMedia> uploadMedia(Tagline tagline) throws IOException, TwitterException {
        List<UploadedMedia> uploadedMediaList = new ArrayList<>();

        Twitter twitter = TwitterInstance.getTwitterInstance();
        if (tagline.getHref() != null) {
            log("Uploading screenshot");
            UploadedMedia screenshot = twitter.tweets().uploadMedia("Screenshot.png", getScreenshot(tagline.getHref()));
            uploadedMediaList.add(screenshot);
            log("Finished uploaded screenshot");
        } else {
            log("No screenshot to upload");
        }

        if (tagline.getBackground() != null) {
            log("Uploading background");
            UploadedMedia screenshot = twitter.tweets().uploadMedia("Background.png", Utils.urlToInputStream(tagline.getBackground()));
            uploadedMediaList.add(screenshot);
            log("Finished uploaded background");
        } else {
            log("No background to upload");
        }

        return uploadedMediaList;
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
