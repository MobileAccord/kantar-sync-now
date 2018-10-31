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
package com.mobileaccord.geopoll.plugins.kantarsyncnow;

import java.io.File;
import java.util.Locale;
import android.os.Environment;
import android.util.Log;

import com.kantarmedia.syncnow.SyncNowDetector;
import com.kantarmedia.syncnow.SyncNowDetector.MotionType;
import com.kantarmedia.syncnow.SyncNowDetector.WatermarkDetectorConfiguration;
import com.kantarmedia.syncnow.SyncNowDetectorFactory;
import com.kantarmedia.syncnow.SyncNowDetectorListener;
import com.kantarmedia.syncnow.SyncNowDetector.UtcAbsoluteDateAndTime;



public class AudioDetector implements SyncNowDetectorListener {

	/** Base filename for recording and log **/
	private static final String BASE_FILE_NAME = Environment.getExternalStorageDirectory() + File.separator + "SyncNowDetector";

	/** SDK wrapper **/
	private SyncNowDetector mDetectorSDK = null;
	private DetectorConfiguration mDetectorConfig = null;

	/* Used to display message in the UI */
	private KantarSyncNow mParent = null;

	public static class DetectorConfiguration
	{
		/** Write log output to local filesystem.
		 Default value is get from the preferences @see#updateSettings **/
		public boolean logEnabled = false;

		/** Record of a wav file is activated.  **/
		public boolean recordEnabled = false;

		/** Default license value is get from the preferences @see#updateSettings **/
		public String license = "";

		/** Default NumIdentifierBits value is get from the preferences @see#updateSettings **/
		public int numIdentifierBits = 0;

		/** Default NumTimeStampBits value is get from the preferences @see#updateSettings **/
		public int numTimeStampBits = 0;

		/** Instance name to put in front of all log in order to distinguish them.
		 *  Also used for output file names. **/
		public String instName = "";

		public DetectorConfiguration(){};
	}


	/**
	 * Audio Detector constructor.
	 *
	 * Additionnal different configuration for the start detection watermark
	 *    *
	 * @throws Exception with createSyncNowDetector if Detector is null return error in StringBuilder
	 */
	public AudioDetector(KantarSyncNow parent, AudioCapture.AudioConfiguration audioConfig, AudioDetector.DetectorConfiguration detectorConfig) throws Exception {
		super();
		mParent = parent;
		mDetectorConfig = detectorConfig;

		WatermarkDetectorConfiguration detectorConfiguration = new WatermarkDetectorConfiguration();
		detectorConfiguration.audioParameters.sampleRate = audioConfig.mSampleRate;
		detectorConfiguration.audioParameters.numBitsPerChannel = audioConfig.mNumBitsPerChannel;
		detectorConfiguration.audioParameters.numChannels = audioConfig.mNumChannels;
		detectorConfiguration.audioParameters.buffLength = audioConfig.mBufferSize;
		detectorConfiguration.algorithmParameters.mode = SyncNowDetector.MODE_LIVE;
		detectorConfiguration.algorithmParameters.numIdentifierBits = detectorConfig.numIdentifierBits;
		detectorConfiguration.algorithmParameters.numTimeStampBits = detectorConfig.numTimeStampBits;
		// Setting the license is mandatory to enable the detection.
		// This is where the detection technology (SyncNow 2G, SyncNow 3G, SNAP or Ink) is configured.
		detectorConfiguration.algorithmParameters.license = detectorConfig.license;
		// Set the listener instance to receive the SDK notification events: onPayload(), onDebug() and onAlarm() 
		detectorConfiguration.algorithmParameters.listener = this;

		// Logging detection events in file
		if (detectorConfig.logEnabled) {
			String FileNameLog = getNextAvailableFileName(BASE_FILE_NAME,".txt");
			if (null != FileNameLog) {
				KantarSyncNow.println("log:" + " " + FileNameLog);
				detectorConfiguration.extraParameters.logFileName = FileNameLog;
			}
		}

		// Record input audio
		if (detectorConfig.recordEnabled) {
			String FileNameInputAudioRecord = getNextAvailableFileName(BASE_FILE_NAME,".wav");
			if (null != FileNameInputAudioRecord) {
				KantarSyncNow.println("Record:" + " " + FileNameInputAudioRecord);
				detectorConfiguration.extraParameters.recordFileName = FileNameInputAudioRecord;
			}
		}

		// Instantiate the SyncNow Detector SDK with the create factory API.
		// This instance is the SDK entry point to access the detection API.
		StringBuilder resultString = new StringBuilder();
		// Instantiate the SDK detector
		mDetectorSDK = SyncNowDetectorFactory.createSyncNowDetector(mParent.cordova.getActivity(), detectorConfiguration, resultString);
		if(null == mDetectorSDK){
			// irrecoverable error: the SDK can not be instantiated		  
			throw new Exception("SyncNow Detector SDK can not be instantiated." + " " + resultString);
		}

		//default mode
		if(resultString.toString().equals("AWM_DEFAULT_DETECTOR")){
			
			onDebug("Warning : Default mode activated (default parameters used instead of customer requirements). Check your license or contact Kantar Media support. Technology:" + " " + resultString);
		}
		else
		{
			onDebug("SyncNow Detector SDK instance creation succeed with technology" + " "+ resultString);
		}

		if(false == mDetectorSDK.setCurrentMotion(MotionType.MOTION_AUTOMATIC)){
			KantarSyncNow.verboseLog(1,"KantarSyncNow::startDetector","## setCurrentMotion failure for detector " + mDetectorConfig.instName);
		}

	}

	public String getVersion()  {
		return mDetectorSDK.getVersion();
	}

	public boolean getWatermarkPresence()  {
		return mDetectorSDK.getWatermarkPresence();
	}

	public boolean getResetHistoricDetection()  {
		return mDetectorSDK.resetHistoricDetection();
	}

	public boolean pushAudioBuffer(byte audioSampleBuffer[], int audioSampleBufferSize){
		return mDetectorSDK.pushAudioBuffer(audioSampleBuffer, audioSampleBufferSize);
	}

	public void stop()  {
		SyncNowDetectorFactory.destroy(mDetectorSDK);
	}


	/**
	 * Callback from SyncNowDetectorListener.
	 **/
	@Override
	public void onDebug(String text) {
		String messsage = mDetectorConfig.instName + mParent.getStringResource("onDebug") + " " + text;
		//mParent.requestPrint(messsage);
		Log.d("Audio Detector", messsage);
	}

	/**
	 * Callback from SyncNowDetectorListener.
	 **/
	@Override
	public void onPayload(PayloadEvent event) {
		String messsage = mDetectorConfig.instName + mParent.getStringResource("onPayload") + " " + convertPayloadtoString(event);
		mParent.requestPrint(messsage);
	}

	/**
	 * Callback from SyncNowDetectorListener.
	 **/
	@Override
	public void onAlarm(AlarmEvent event) {
		// test to avoid displaying the confidence values
		if (!((SyncNowDetectorListener.AlarmEventType.TYPE_INFO == event.type) && (SyncNowDetectorListener.AlarmEventCode.INFO_CONFIDENCE_VALUE == event.code))) {
			String messsage = mDetectorConfig.instName + mParent.getStringResource("onAlarm") + " " + convertAlarmtoString(event);
			mParent.requestPrint(messsage);
		}
	}


	/**
	 * Payload converter.
	 *
	 * @param event a payload event instance
	 * @return the payload event converted into a human readable string
	 */
	private String convertPayloadtoString(PayloadEvent event) {
		String retValue = null;
		String timeConvertedStr = "";

		if(PayloadType.TYPE_IDENTIFIED == event.payloadType) {

			if((PayloadEvent.VALUE_NOT_DEFINED != event.contentID.msb) && (PayloadEvent.VALUE_NOT_DEFINED == event.timeStamp)) {
				// ****** Static ID notification *****
				retValue = String.format(Locale.ROOT, "StaticID detected: %s Confidence: %.2f Technology: %s", event.contentID.value, event.confidence, event.awmTechnology);
				//If NOT using BlueInk technology, you can get contentID as Long with event.contentID.lsb
			}
			else if((PayloadEvent.VALUE_NOT_DEFINED != event.timeStamp) && (PayloadEvent.VALUE_NOT_DEFINED == event.contentID.msb)) {
				// ****** Timestamp notification *****
				UtcAbsoluteDateAndTime utcTime;
				if (null != mDetectorSDK) { // It may happen the detector is destroyed and some late notifications occur
					utcTime = mDetectorSDK.translateIntoAbsoluteDateAndTime(event.timeStamp);
					timeConvertedStr = String.format(Locale.ROOT, "(UTC %d-%02d-%02d %02d:%02d:%02d)", utcTime.year, utcTime.month, utcTime.day, utcTime.hour, utcTime.minute, utcTime.second);
				}
				retValue = String.format(Locale.ROOT, "Timestamp detected: %.2fs\n%s Confidence: %.2f Technology: %s", event.timeStamp, timeConvertedStr, event.confidence, event.awmTechnology);
			}
			else {
				retValue = "Unknown TYPE_IDENTIFIER received";
			}
		}
		else if(SyncNowDetectorListener.PayloadType.TYPE_NOT_IDENTIFIED == event.payloadType) {
			retValue = "Content not marked, Technology : " + " " + event.awmTechnology;
		}
		else if(SyncNowDetectorListener.PayloadType.TYPE_MARKED_BUT_NOT_IDENTIFIED == event.payloadType) {
			retValue = "Content marked but not identified, Technology : " + " " + event.awmTechnology;
		}
		return retValue;
	}

	/**
	 * Alarm event conversion.
	 *
	 * @param event alarm event to be converted
	 * @return a string representing the alarm event
	 */
	private String convertAlarmtoString(AlarmEvent event) {
		String res = null;
		if ((SyncNowDetectorListener.AlarmEventType.TYPE_INFO == event.type)) {
			res = ""; // Ignoring to keep the log buffer smaller
			if (KantarSyncNow.sVerbose) {
				res = "Confidence:" + event.message;
			}
		}
		else { // Others alerts
			res = "alert: " + event.message;
		}
		return res;
	}

	/**
	 * Computes the next available file name.
	 *
	 * This is an helper method to compute the next available
	 * file names for the logs and the audio input recording.
	 *
	 * @param firstFileName the file name of the first file to create 
	 * @return the next available file name
	 */
	private String getNextAvailableFileName(String firstFileName, String extension) {
		String nextValidName = null;
		int numFile = 1;
		nextValidName = firstFileName + "_" + mDetectorConfig.instName + "_" + numFile + extension;
		File file = new File(nextValidName);

		// Loop till a non used file name is found
		while (file.exists()) {
			nextValidName = firstFileName + "_" + mDetectorConfig.instName + "_" + numFile + extension;
			file = new File(nextValidName);
			numFile++;
		}

		@SuppressWarnings("unused")
		boolean deleted = file.delete();
		return nextValidName;
	}


}
