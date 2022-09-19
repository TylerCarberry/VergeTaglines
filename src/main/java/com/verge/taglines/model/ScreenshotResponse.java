package com.verge.taglines.model;

public class ScreenshotResponse {

    public String url;
    public String extractedHtml;
    public String extractedText;

    public ScreenshotResponse() {
    }

    public ScreenshotResponse(String url, String extractedHtml, String extractedText) {
        this.url = url;
        this.extractedHtml = extractedHtml;
        this.extractedText = extractedText;
    }

}
