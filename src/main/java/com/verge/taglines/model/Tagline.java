package com.verge.taglines.model;

import java.util.Objects;

public class Tagline {

    private String text;
    private String href;
    private String background;

    public Tagline() {
    }

    public Tagline(String text, String href, String background) {
        this.text = text;
        this.href = href;
        this.background = background;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tagline tagline1 = (Tagline) o;

        if (!Objects.equals(text, tagline1.text)) return false;
        if (!Objects.equals(href, tagline1.href)) return false;
        return Objects.equals(background, tagline1.background);
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (href != null ? href.hashCode() : 0);
        result = 31 * result + (background != null ? background.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Tagline{" +
                "text='" + text + '\'' +
                ", href='" + href + '\'' +
                ", background='" + background + '\'' +
                '}';
    }
}
