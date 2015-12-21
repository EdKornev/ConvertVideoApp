package com.ub.convertvideoapp.app;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;
import net.ypresto.androidtranscoder.engine.*;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.utils.MediaExtractorUtils;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by Eduard on 16.12.2015.
 */
public class Engine {
    private static final String TAG = "MediaTranscoderEngine";
    private static final double PROGRESS_UNKNOWN = -1.0D;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10L;
    private static final long PROGRESS_INTERVAL_STEPS = 10L;
    private FileDescriptor mInputFileDescriptor;
    private TrackTranscoder mVideoTrackTranscoder;
    private TrackTranscoder mAudioTrackTranscoder;
    private MediaExtractor mExtractor;
    private MediaMuxer mMuxer;
    private volatile double mProgress;
    private MediaTranscoderEngine.ProgressCallback mProgressCallback;
    private long mDurationUs;

    public Engine() {
    }

    public void setDataSource(FileDescriptor fileDescriptor) {
        this.mInputFileDescriptor = fileDescriptor;
    }

    public MediaTranscoderEngine.ProgressCallback getProgressCallback() {
        return this.mProgressCallback;
    }

    public void setProgressCallback(MediaTranscoderEngine.ProgressCallback progressCallback) {
        this.mProgressCallback = progressCallback;
    }

    public double getProgress() {
        return this.mProgress;
    }

    public void transcodeVideo(String outputPath, MediaFormatStrategy formatStrategy) throws IOException {
        if(outputPath == null) {
            throw new NullPointerException("Output path cannot be null.");
        } else if(this.mInputFileDescriptor == null) {
            throw new IllegalStateException("Data source is not set.");
        } else {
            try {
                this.mExtractor = new MediaExtractor();
                this.mExtractor.setDataSource(this.mInputFileDescriptor);
                this.mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                this.setupMetadata();
                this.setupTrackTranscoders(formatStrategy);
                this.runPipelines();
                this.mMuxer.stop();
            } finally {
                try {
                    if(this.mVideoTrackTranscoder != null) {
                        this.mVideoTrackTranscoder.release();
                        this.mVideoTrackTranscoder = null;
                    }

                    if(this.mAudioTrackTranscoder != null) {
                        this.mAudioTrackTranscoder.release();
                        this.mAudioTrackTranscoder = null;
                    }

                    if(this.mExtractor != null) {
                        this.mExtractor.release();
                        this.mExtractor = null;
                    }
                } catch (RuntimeException var12) {
                    throw new Error("Could not shutdown extractor, codecs and muxer pipeline.", var12);
                }

                try {
                    if(this.mMuxer != null) {
                        this.mMuxer.release();
                        this.mMuxer = null;
                    }
                } catch (RuntimeException var11) {
                    Log.e("MediaTranscoderEngine", "Failed to release muxer.", var11);
                }

            }

        }
    }

    private void setupMetadata() throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(this.mInputFileDescriptor);
        String rotationString = mediaMetadataRetriever.extractMetadata(24);

        try {
            this.mMuxer.setOrientationHint(Integer.parseInt(rotationString));
        } catch (NumberFormatException var5) {
            ;
        }

        try {
            this.mDurationUs = Long.parseLong(mediaMetadataRetriever.extractMetadata(9)) * 1000L;
        } catch (NumberFormatException var4) {
            this.mDurationUs = -1L;
        }

        Log.d("MediaTranscoderEngine", "Duration (us): " + this.mDurationUs);
    }

    private void setupTrackTranscoders(MediaFormatStrategy formatStrategy) {
        MediaExtractorUtils.TrackResult trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(this.mExtractor);
        MediaFormat videoOutputFormat = formatStrategy.createVideoOutputFormat(trackResult.mVideoTrackFormat);
        MediaFormat audioOutputFormat = formatStrategy.createAudioOutputFormat(trackResult.mAudioTrackFormat);
        if(videoOutputFormat == null && audioOutputFormat == null) {
            throw new InvalidOutputFormatException("MediaFormatStrategy returned pass-through for both video and audio. No transcoding is necessary.");
        } else {
            QueuedMuxer queuedMuxer = new QueuedMuxer(this.mMuxer, new QueuedMuxer.Listener() {
                public void onDetermineOutputFormat() {

                }
            });
            if(videoOutputFormat == null) {
                this.mVideoTrackTranscoder = new PassThroughTrackTranscoder(this.mExtractor, trackResult.mVideoTrackIndex, queuedMuxer, QueuedMuxer.SampleType.VIDEO);
            } else {
                this.mVideoTrackTranscoder = new VideoTrackTranscoder(this.mExtractor, trackResult.mVideoTrackIndex, videoOutputFormat, queuedMuxer);
            }

            try {
                this.mVideoTrackTranscoder.setup();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            if(audioOutputFormat == null) {
                this.mAudioTrackTranscoder = new PassThroughTrackTranscoder(this.mExtractor, trackResult.mAudioTrackIndex, queuedMuxer, QueuedMuxer.SampleType.AUDIO);
                this.mAudioTrackTranscoder.setup();
                this.mExtractor.selectTrack(trackResult.mVideoTrackIndex);
                this.mExtractor.selectTrack(trackResult.mAudioTrackIndex);
            } else {
                throw new UnsupportedOperationException("Transcoding audio tracks currently not supported.");
            }
        }
    }

    private void runPipelines() {
        long loopCount = 0L;
        if(this.mDurationUs <= 0L) {
            double stepped = -1.0D;
            this.mProgress = stepped;
            if(this.mProgressCallback != null) {
                this.mProgressCallback.onProgress(stepped);
            }
        }

        while(!this.mVideoTrackTranscoder.isFinished() || !this.mAudioTrackTranscoder.isFinished()) {
            boolean var11 = this.mVideoTrackTranscoder.stepPipeline() || this.mAudioTrackTranscoder.stepPipeline();
            ++loopCount;
            if(this.mDurationUs > 0L && loopCount % 10L == 0L) {
                double e = this.mVideoTrackTranscoder.isFinished()?1.0D:Math.min(1.0D, (double)this.mVideoTrackTranscoder.getWrittenPresentationTimeUs() / (double)this.mDurationUs);
                double audioProgress = this.mAudioTrackTranscoder.isFinished()?1.0D:Math.min(1.0D, (double)this.mAudioTrackTranscoder.getWrittenPresentationTimeUs() / (double)this.mDurationUs);
                double progress = (e + audioProgress) / 2.0D;
                this.mProgress = progress;
                if(this.mProgressCallback != null) {
                    this.mProgressCallback.onProgress(progress);
                }
            }

            if(!var11) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var10) {
                    ;
                }
            }
        }

    }

    public interface ProgressCallback {
        void onProgress(double var1);
    }
}
