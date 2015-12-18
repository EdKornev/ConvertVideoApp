package com.ub.convertvideoapp.app;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

/**
 * Created by Eduard on 16.12.2015.
 */
public class CustomFormatStrategy implements MediaFormatStrategy {

    private int width= 640;
    private int height = 480;
    private double aspect_ratio;
    private static final int DEFAULT_BITRATE = 1546000;
    private final int mBitRate;
    private String type = "video/avc";

    public CustomFormatStrategy() {
        this.mBitRate = DEFAULT_BITRATE;
    }

    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
            MediaFormat format = MediaFormat.createVideoFormat(type, getWidth(), getHeight());
            format.setInteger("bitrate", this.mBitRate);
            format.setInteger("frame-rate", 30);
            format.setInteger("i-frame-interval", 5);
            format.setInteger("color-format", MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
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

    public double getAspect_ratio() {
        return aspect_ratio;
    }

    public void setAspect_ratio(double aspect_ratio) {
        this.aspect_ratio = aspect_ratio;
    }
}
