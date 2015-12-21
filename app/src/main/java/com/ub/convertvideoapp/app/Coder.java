package com.ub.convertvideoapp.app;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import net.ypresto.androidtranscoder.engine.MediaTranscoderEngine;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Eduard on 16.12.2015.
 */
public class Coder {

    private ThreadPoolExecutor mExecutor;
    private static volatile Coder instance;

    private Coder() {
        this.mExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "MediaTranscoder-Worker");
            }
        });
    }

    public static Coder getInstance() {
        if(instance == null) {
            synchronized(Coder.class) {
                if(instance == null) {
                    instance = new Coder();
                }
            }
        }

        return instance;
    }

    /**
     * From file
     * @param inPath
     * @param width
     * @param height
     * @param type - type which support encoder android sdk
     * @param format - right end of format (ex: .mp4/.3gp)
     * @param listener - interface for callback if you need
     * @throws IOException
     */
    public void transcodeVideo(String inPath, int width, int height, String type, String format, final Coder.Listener listener) throws IOException {
        File file = File.createTempFile("transcode_test_" + Calendar.getInstance().getTimeInMillis() + format, inPath);

        double ratio = getRatio(inPath, listener);

        if (ratio > 0) {
            width = Double.valueOf(ratio * height).intValue();
        }

        FileInputStream fileInputStream = null;

        FileDescriptor inFileDescriptor;
        try {
            fileInputStream = new FileInputStream(inPath);
            inFileDescriptor = fileInputStream.getFD();
        } catch (IOException var10) {
            listener.onTranscodeFailed(var10);
            if(fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException var9) {
                    Log.e("MediaTranscoder", "Can\'t close input stream: ", var9);
                    listener.onTranscodeFailed(var9);
                }
            }

            throw var10;
        }

        CustomFormatStrategy strategy = new CustomFormatStrategy();
        strategy.setHeight(height);
        strategy.setWidth(width);
        strategy.setType(type);

        this.transcodeVideo(inFileDescriptor, file.getAbsolutePath(), strategy, listener);
    }

    /**
     * From content
     * @param inFileDescriptor
     * @param outPath
     * @param outFormatStrategy
     */
    public void transcodeVideo(final FileDescriptor inFileDescriptor, final String outPath, final MediaFormatStrategy outFormatStrategy, final Coder.Listener listener) {
        this.mExecutor.execute(new Runnable() {
            public void run() {
                Looper looper = Looper.myLooper();
                if(looper == null) {
                    looper = Looper.getMainLooper();
                }

                final Handler handler = new Handler(looper);

                try {
                    Engine exception = new Engine();
                    exception.setProgressCallback(new MediaTranscoderEngine.ProgressCallback() {
                        public void onProgress(final double progress) {
                            handler.post(new Runnable() {
                                public void run() {
                                    listener.onTranscodeProgress(progress);
                                }
                            });
                        }
                    });
                    exception.setDataSource(inFileDescriptor);
                    exception.transcodeVideo(outPath, outFormatStrategy);
                } catch (IOException var3) {
                    Log.w("MediaTranscoder", "Transcode failed: input file (fd: " + inFileDescriptor.toString() + ") not found" + " or could not open output file (\'" + outPath + "\') .", var3);
                    listener.onTranscodeFailed(var3);
                } catch (RuntimeException var4) {
                    Log.e("MediaTranscoder", "Fatal error while transcoding, this might be invalid format or bug in engine or Android.", var4);
                    listener.onTranscodeFailed(var4);
                } catch (Exception var5) {
                    listener.onTranscodeFailed(var5);
                }

                handler.post(new Runnable() {
                    public void run() {
                        listener.onTranscodeCompleted();
                    }
                });
            }
        });
    }

    private double  getRatio(String path, Coder.Listener listener) {
        MediaMetadataRetriever retriever = new  MediaMetadataRetriever();
        Bitmap bmp = null;

        int videoHeight = 0;
        int videoWidth = 0;

        try {
            retriever.setDataSource("...location of your video file");
            bmp = retriever.getFrameAtTime();
            videoHeight=bmp.getHeight();
            videoWidth=bmp.getWidth();

            return videoWidth/videoHeight;
        } catch (IllegalArgumentException e) {
            listener.onTranscodeFailed(e);
        }

        return -1;
    }

    public interface Listener {
        void onTranscodeProgress(double var1);

        void onTranscodeCompleted();

        void onTranscodeFailed(Exception var1);
    }
}
