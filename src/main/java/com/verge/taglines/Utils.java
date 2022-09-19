package com.verge.taglines;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

public class Utils {

    public static char randomLetter() {
        Random r = new Random();
        return (char)(r.nextInt(26) + 'a');
    }

    public static void saveToFile(String url, String path) throws IOException {
        URL website = new URL(url);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(path);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    public static InputStream urlToInputStream(String url) throws IOException {
        return new URL(url).openStream();
    }

    public static InputStream pngToInputStream(BufferedImage image) throws IOException {
        BufferedImage croppedImage = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(croppedImage, "png", stream);
        return new ByteArrayInputStream(stream.toByteArray());
    }
}
