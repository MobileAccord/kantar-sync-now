/**
 * Copyright (c) 2017 Kantar S.A.S. All rights reserved.
 *
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Kantar S.A.S. and is confidential in nature.
 *
 * Under no circumstances is this software to be combined with any Open Source
 * Software in any way or placed under an Open Source License of any type
 * without the express written permission of Kantar S.A.S.
 *
 */
package com.kantarmedia.syncnow.example.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class AudioCapture extends Thread {
    // Constant values:
    private static final int BUFFER_SIZE_MIN = 1024;

    // Member to interact with the parent
    private KantarSyncNow mParent = null;

    // Audio capture encoding configuration
    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT;

    // Audio record and configuration member objects
    private AudioRecord mAudio = null; // Audio recording Android class API
    private AudioConfiguration mAudioConfiguration = null;
    private byte[] mBuffer = null;    // PCM input audio buffer

    /*
     * Sample rate is hard coded to 44100Hz, since it is the Android audio requirement
     * for devices to support. Official Android states: "44100Hz is currently the only
     * rate that is guaranteed to work on all devices"
     * Ref: http://developer.android.com/reference/android/media/AudioRecord.html
     */
    public class AudioConfiguration {
        // Audio capture configuration
        int mChannels = AudioFormat.CHANNEL_IN_MONO;
        int mSampleRate = 44100;   // Sample rate in Hz
        int mNumChannels = (AudioFormat.CHANNEL_IN_MONO == mChannels) ? 1 : 2;
        int mNumBitsPerChannel = (AudioFormat.ENCODING_PCM_16BIT == mEncoding) ? 16 : 8;
        int mBufferSize = 0;

        public AudioConfiguration(){};
    }


    /**
     * Audio capture constructor.
     *
     * Create and configure the AudioRecord object
     *    *
     * @throws Exception in case of any initialization failure
     */
    public AudioCapture (KantarSyncNow parent) throws Exception {
        mParent = parent;
        // Creation AudioConfiguration with the defaults configuration
        mAudioConfiguration = new AudioConfiguration();

        int status = 0;

        // Get the input buffer minimum size
        mAudioConfiguration.mBufferSize = AudioRecord.getMinBufferSize(mAudioConfiguration.mSampleRate, mAudioConfiguration.mChannels, mEncoding);
        if (mAudioConfiguration.mBufferSize <= 0) {
            throw new Exception("AudioCapture ERROR: Min buffer size must exceed 0");
        }
        else {
            // Check buffer size
            if (mAudioConfiguration.mBufferSize <= BUFFER_SIZE_MIN) {
                mAudioConfiguration.mBufferSize = BUFFER_SIZE_MIN*mAudioConfiguration.mChannels;
            }
        }

        // Audio recording initialization
        mBuffer = new byte[mAudioConfiguration.mBufferSize];
        mAudio = new AudioRecord(AudioSource.MIC, mAudioConfiguration.mSampleRate, mAudioConfiguration.mChannels, mEncoding, mAudioConfiguration.mBufferSize);
        status = mAudio.getState();
        if(status == AudioRecord.STATE_INITIALIZED) {
            // Start the audio recording
            mAudio.startRecording();
        }
        else {
            throw new Exception("Expected state=STATE_INITIALIZED, current state=" + status);
        }
    }

    /**
     * Audio Capture.
     *
     * Stop thread
     *
     */
    public void finalize (){
        if( null != mAudio) {
            mAudio.stop();
            mAudio.release();
            mAudio = null;
        }
    }

    /**
     * Audio configuration.
     *
     * Getter AudioConfiguation
     *
     */
    public AudioConfiguration getAudioConfiguration() {
        return mAudioConfiguration;
    }


    /**
     * Audio capture thread entry point.
     *
     * This worker thread reads audio buffers from microphone and provides the audio samples to the SDK stack.
     *
     * The thread terminates when interrupted or when an error occurred.
     *
     */
    public void run() {
        int status = 0;
        try {
            while (!isInterrupted()) {
                if (mAudioConfiguration.mBufferSize > 0) {
                    status = mAudio.read(mBuffer, 0, mAudioConfiguration.mBufferSize);
                    if((0 == status) || (AudioRecord.ERROR_INVALID_OPERATION == status)) {
                        // The mic resource may be already in use by another App..
                        // In some devices ERROR_INVALID_OPERATION is returned, in others 0 is returned..
                        throw new Exception("read() error: mic resource already in use - ERROR_INVALID_OPERATION");
                    }
                    else if(AudioRecord.ERROR_BAD_VALUE == status) {
                        throw new Exception("read() error: mic resource already in use - ERROR_BAD_VALUE");
                    }
                    else {
                        if (null != mParent) {
                            if (true != mParent.pushAudioBuffer(mBuffer, mAudioConfiguration.mBufferSize))
                                throw new Exception("error: pushAudioBuffer()=false");
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            KantarSyncNow.verboseLog(1,"AudioCapture","## run(): Exceptions in audio loop - msg=" + e.getMessage());
        }
    }
}
