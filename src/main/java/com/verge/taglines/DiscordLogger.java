package com.verge.taglines;

import com.diluv.schoomp.Webhook;
import com.diluv.schoomp.message.Message;
import com.diluv.schoomp.message.embed.Embed;
import com.diluv.schoomp.message.embed.Footer;
import com.verge.taglines.model.EnvironmentVariable;
import com.verge.taglines.model.Tagline;
import org.springframework.lang.Nullable;

import java.io.IOException;

public class DiscordLogger {

    public DiscordLogger() {}

    public void postOnDiscord(Tagline tagline, String twitterUrl) throws IOException {
        String title = replaceIfEmpty(tagline.getText(), "No text");
        String body = replaceIfEmpty(tagline.getHref(), "No href");
        String background = replaceIfEmpty(tagline.getBackground(), "https://via.placeholder.com/150x150.png?text=No+background");

        String webhookId = EnvironmentVariable.DISCORD_WEBHOOK_ID.getValue();
        String webhookToken = EnvironmentVariable.DISCORD_WEBHOOK_TOKEN.getValue();

        final Webhook webhook = new Webhook(String.format("https://discord.com/api/webhooks/%s/%s", webhookId, webhookToken), "@VergeTaglines");
        webhook.sendMessage(new Message().addEmbed(new Embed().setImage(background).setTitle(title).setUrl(twitterUrl).setFooter(new Footer(body))));
    }

    public static String replaceIfEmpty(@Nullable String str, String defaultValue) {
        if (isNullOrEmpty(str)) {
            return defaultValue;
        }
        return str;
    }

    public static boolean isNullOrEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }

}
