package com.ub.convertvideoapp.app;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

/**
 * Created by Eduard on 16.12.2015.
 */
public class CustomFormatStrategy implements MediaFormatStrategy {

    private int width= 640;
    private int height = 480;
    private double aspect_ratio;
    private static final int DEFAULT_BITRATE = 500000;//1546000;
    private final int mBitRate;
    private String type = "video/avc";

    public CustomFormatStrategy() {
        this.mBitRate = DEFAULT_BITRATE;
    }

    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
            MediaFormat format = MediaFormat.createVideoFormat("video/mp4v-es", getWidth(), getHeight());
            format.setInteger("bitrate", 500000);//this.mBitRate);
            format.setInteger("frame-rate", 30);
            format.setInteger("i-frame-interval", 5);
            format.setInteger("color-format", MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            return format;
//        }
    }

    private static int selectColorFormat(String mimeType) {
        int size = MediaCodecList.getCodecCount();
        for (int i = 0; i < size; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            for (String type : info.getSupportedTypes()) {
                if (type.equals(mimeType)) {
                    MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(mimeType);
                    for (int j = 0; j < capabilities.colorFormats.length; j++) {
                        int colorFormat = capabilities.colorFormats[j];
                        if (isRecognizedFormat(colorFormat)) {
                            return colorFormat;
                        }
                    }
                }
            }
        }
        return 0;   // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
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
