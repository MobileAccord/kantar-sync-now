package com.mobileaccrod.geopoll.plugins.kantarsyncnow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.kantarmedia.syncnow.SyncNowDetector;
import com.mobileaccrod.geopoll.R;



import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class echoes a string called from JavaScript.
 */
public class KantarSyncNow extends CordovaPlugin {

    public static final String PREF_KEY_LOG = "log";
    public static final String PREF_KEY_LICENSE = "license";
    public static final String PREF_KEY_CONTENT_ID_BITS_LENGTH = "numIdentifierBits";
    public static final String PREF_KEY_TIMESTAMP_BITS_LENGTH = "numTimeStampBits";
    public static final String PREF_KEY_RESET_SETTINGS = "resetSettingsPref";

    private Vector<AudioDetector> mDetectors = null;
    /**
     * Static variable for log trace
     */
    public static boolean sVerbose;

    /**
     * Will capture on Start
     **/
    private boolean mAutoStart = true;

    /**
     * Number of detector 0, 1 or 2
     */
    private static int sNbDetectorToRun;

    // Members for audio capture and detection
    /**
     * Detection SDK wrapper and configuration
     **/
    private Vector<AudioDetector> mDetectors = null;
    private Vector<AudioDetector.DetectorConfiguration> mDetectorConfigs = null;

    /**
     * Audio Capture Thread
     **/
    private AudioCapture mAudioCapture = null;

    /**
     * Runtime permission requests
     **/
    private static final int REQUEST_STARTDETECTION_PERMISSIONS = 123;
    private static final int REQUEST_STARTRECORD_PERMISSIONS = 124;


    public KantarSyncNow() {
        //init verbose with default preference in XML default_values.xml
        sVerbose = getResources().getBoolean(R.bool.verbose);
        //init nbDetectorToRun with default preference in XML default_values.xml
        sNbDetectorToRun = getResources().getInteger(R.integer.nbDetectorToRun);
        //check if nb detector is 1 or 2
        if (sNbDetectorToRun < 1 || sNbDetectorToRun > 2) {
            sNbDetectorToRun = 2;
        }

        mDetectors = new Vector<AudioDetector>(sNbDetectorToRun, 1);

        mDetectorConfigs = new Vector<AudioDetector.DetectorConfiguration>(sNbDetectorToRun, 1);
        mDetectorConfigs.add(new AudioDetector.DetectorConfiguration());
        mDetectorConfigs.add(new AudioDetector.DetectorConfiguration());
    }

    /**
     * Run time permissions request (since API 23)
     *
     * @param requestCode int used to handle pending action at onRequestPermissionsResult callback
     * @return true if permissions are already granted, false if not (in this case a requestPermissions procedure is launched using the requestCode provided)
     **/
    private synchronized boolean checkPermissions(int requestCode) {
        List<String> permissionsList = new ArrayList<String>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.RECORD_AUDIO);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    /**
     * MainActivity offices as a proxy from the AudioCapture to the AudioDetectors
     **/
    public boolean pushAudioBuffer(byte audioSampleBuffer[], int audioSampleBufferSize) {
        boolean pushed = true;

        for (int i = 0; i < mDetectors.size(); i++) {
            pushed = mDetectors.elementAt(i).pushAudioBuffer(audioSampleBuffer, audioSampleBufferSize);
            if (!pushed) {
                break;
            }
        }
        return pushed;
    }

    /**
     * Displays text to screen asynchronously via the handler.
     *
     * @param text string sent to the handler
     **/
    public void requestPrint(String text) {
        if (null != text) {
            Message msg = new Message();
            msg.obj = text;
            msg.what = sRequestPrint;

            Log.d("Kantar Printer", msg.toString())
        }
    }

    private boolean updateSettings() {
        boolean retCode = true;
        Resources resourcesInst = getResources();
        String stringDefaultValue = null;
        String errorInfo = "";

        try {
            //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            int ressourceId;

            for (int i = 0; i < sNbDetectorToRun; i++) {
                // Read boolean values: log
                boolean tempBoolDefaultValue = resourcesInst.getBoolean(R.bool.logDefaultValue);
                mDetectorConfigs.elementAt(i).logEnabled = true;

                // Read license value
                errorInfo = this.cordova.getActivity().getString(R.string.error_license);
                ressourceId = this.cordova.getActivity().getResources().getIdentifier("licenseDefaultValue" + i, "string", getPackageName());
                stringDefaultValue = resourcesInst.getString(ressourceId);
                mDetectorConfigs.elementAt(i).license = stringDefaultValue;

                // Read content ID bits length
                errorInfo = this.cordova.getActivity().getString(R.string.error_identifier);
                ressourceId = this.cordova.getActivity().getResources().getIdentifier("numIdentifierBitsDefaultValue" + i, "integer", getPackageName());
                stringDefaultValue = resourcesInst.getString(ressourceId);
                mDetectorConfigs.elementAt(i).numIdentifierBits = Integer.valueOf(stringDefaultValue);

                // Read time stamp bits length
                errorInfo = this.cordova.getActivity().getString(R.string.error_timestamp);
                ressourceId = this.cordova.getActivity().getResources().getIdentifier("numTimeStampBitsDefaultValue" + i, "integer", getPackageName());
                stringDefaultValue = resourcesInst.getString(ressourceId);
                mDetectorConfigs.elementAt(i).numTimeStampBits = Integer.valueOf(stringDefaultValue);
            }
        } catch (Exception ex) {
            verboseLog(0, "## MainActivity:updateSettings", "EXCEPTION: " + errorInfo + " Msg: " + ex.getMessage());
            retCode = false;
        }

        return retCode;
    }

    /**
     * SDK starter.
     * <p>
     * The SDK is configured and the audio capture thread is launched.
     * From this point the worker thread provides the audio samples
     * to the SDK detection and the callbacks (ex: onPayload) may be called.
     *
     * @param isRecordEnabled true to enable audio input recording, false otherwise
     * @see AudioDetector
     */
    private synchronized boolean startDetectors(boolean isRecordEnabled) {


        // Update detection parameters from preferences before configuring the SDK
        if (false == updateSettings()) {
            Toast.makeText(this.cordova.getActivity(), this.cordova.getActivity().getString(R.string.invalid_settings), Toast.LENGTH_LONG).show();
            return false;
        }


        // Create and initialize the AudioCapture
        try {
            mAudioCapture = new AudioCapture(this);
        } catch (Exception e) {
            verboseLog(0, "AudioCapture", "## run(): Exceptions in init audio capture - msg=" + e.getMessage());
            Toast.makeText(this.cordova.getActivity(), "AudioCapture ERROR:" + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        AudioCapture.AudioConfiguration audioConfig = mAudioCapture.getAudioConfiguration();

        int isError = 0;
        for (int i = 0; i < sNbDetectorToRun; i++) {
            try {
                mDetectorConfigs.elementAt(i).recordEnabled = isRecordEnabled;
                mDetectorConfigs.elementAt(i).instName = "Inst" + i;
                mDetectors.add(new AudioDetector(this, audioConfig, mDetectorConfigs.elementAt(i)));
            } catch (Exception e) {
                verboseLog(0, "AudioDetector", "## run(): Exceptions in init AudioDetector " + i + " - msg=" + e.getMessage());
                Toast.makeText(this, "AudioDetector " + i + " ERROR:" + e.getMessage(), Toast.LENGTH_LONG).show();
                isError++;
            }
        }

        //Don't start detection if a bad license
        if (isError > 0) {
            stopDetection();
            return false;
        }

        // Just display the SDK version on the UI
        this.requestPrint("Version: " + mDetectors.elementAt(0).getVersion());

        mAudioCapture.setPriority(Thread.MAX_PRIORITY);
        mAudioCapture.start();
        return true; // Everything is OK
    }

    public static void verboseLog(int nbLog, String tag, String message) {
        if (sVerbose) {
            if (nbLog == 0) {
                Log.d(tag, message);
            } else if (nbLog == 1) {
                Log.e(tag, message);
            } else {
                Log.w(tag, message);
            }
        }
    }

    private synchronized void stopDetection() {
        synchronized (this) {

            destroyCaptureAndDetectors();
        }
    }

    /**
     * Kills the audio capture thread and the detectors
     * <p>
     * This method will block until the worker thread is stopped.
     **/
    private synchronized void destroyCaptureAndDetectors() {
        if (null != mAudioCapture) {
            mAudioCapture.interrupt();
            if (mAudioCapture.isAlive()) {
                try {
                    mAudioCapture.join();
                } catch (InterruptedException e) {
                    if (sVerbose) {
                        println(getString(R.string.error));
                        e.printStackTrace();
                    }
                }
            }
            mAudioCapture.finalize();
            mAudioCapture = null;
        }
    }
    /**
     * Wrapper around startDetector() managing the UI buttons enabling.
     **/
    private synchronized void startDetection() {
        synchronized (this) {

            if (true == startDetectors(false/*record disabled*/)) {
                // Update the UI in "stop" mode
                     // Enable watermark get presence button
            }
            else {
                // Detection start failed: do not change UI
                verboseLog(2,"MainActivity::startDetection", "## start detection failure");
            }
        }
    }
}
