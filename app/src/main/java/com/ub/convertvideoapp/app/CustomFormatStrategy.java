package com.ub.convertvideoapp.app;

import android.media.MediaFormat;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

/**
 * Created by Eduard on 16.12.2015.
 */
public class CustomFormatStrategy implements MediaFormatStrategy {

    private int width= 1280;
    private int height = 720;
    private static final int DEFAULT_BITRATE = 8000000;
    private final int mBitRate;
    private String type = "video/avc";

    public CustomFormatStrategy() {
        this.mBitRate = DEFAULT_BITRATE;
    }

    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
            MediaFormat format = MediaFormat.createVideoFormat(type, getWidth(), getHeight());
            format.setInteger("bitrate", this.mBitRate);
            format.setInteger("frame-rate", 30);
            format.setInteger("i-frame-interval", 3);
            format.setInteger("color-format", 2130708361);
            return format;
//        }
    }

    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
