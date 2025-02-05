package wd4j.impl.webdriver.type.browsingContext.parameters;

public class ImageFormat {
    public final String type;
    private final float quality;

    public ImageFormat(String type, float quality) {
        this.type = type;
        this.quality = quality;
    }

    public String getType() {
        return type;
    }

    public float getQuality() {
        return quality;
    }
}
