package de.bund.zrb.video;

import java.nio.file.Path;

/**
 * Value object containing all parameters for a recording session.
 * Immutable configuration for recorder backends.
 */
public final class RecordingProfile {
    
    private final String source;
    private final Path outputFile;
    private final int width;
    private final int height;
    private final int fps;
    private final String videoCodec;
    private final String audioCodec;
    
    private RecordingProfile(Builder builder) {
        this.source = builder.source;
        this.outputFile = builder.outputFile;
        this.width = builder.width;
        this.height = builder.height;
        this.fps = builder.fps;
        this.videoCodec = builder.videoCodec;
        this.audioCodec = builder.audioCodec;
    }
    
    public String getSource() {
        return source;
    }
    
    public Path getOutputFile() {
        return outputFile;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getFps() {
        return fps;
    }
    
    public String getVideoCodec() {
        return videoCodec;
    }
    
    public String getAudioCodec() {
        return audioCodec;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private String source;
        private Path outputFile;
        private int width = 1920;
        private int height = 1080;
        private int fps = 15;
        private String videoCodec;
        private String audioCodec;
        
        private Builder() {}
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder outputFile(Path outputFile) {
            this.outputFile = outputFile;
            return this;
        }
        
        public Builder width(int width) {
            this.width = width;
            return this;
        }
        
        public Builder height(int height) {
            this.height = height;
            return this;
        }
        
        public Builder fps(int fps) {
            this.fps = fps;
            return this;
        }
        
        public Builder videoCodec(String videoCodec) {
            this.videoCodec = videoCodec;
            return this;
        }
        
        public Builder audioCodec(String audioCodec) {
            this.audioCodec = audioCodec;
            return this;
        }
        
        public RecordingProfile build() {
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("source must not be empty");
            }
            if (outputFile == null) {
                throw new IllegalArgumentException("outputFile must not be null");
            }
            if (fps <= 0) {
                throw new IllegalArgumentException("fps must be > 0");
            }
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("width and height must be > 0");
            }
            return new RecordingProfile(this);
        }
    }
    
    @Override
    public String toString() {
        return "RecordingProfile{" +
                "source='" + source + '\'' +
                ", outputFile=" + outputFile +
                ", width=" + width +
                ", height=" + height +
                ", fps=" + fps +
                ", videoCodec='" + videoCodec + '\'' +
                ", audioCodec='" + audioCodec + '\'' +
                '}';
    }
}
