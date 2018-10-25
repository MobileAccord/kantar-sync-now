package com.mobileaccrod.geopoll.plugins.kantarsyncnow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.kantarmedia.syncnow.SyncNowDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class echoes a string called from JavaScript.
 */
public class KantarSyncNow extends CordovaPlugin {

	private Vector<AudioDetector> mDetectors = null;
	/** Static variable for log trace */
	public static boolean sVerbose;

	/** Will capture on Start **/
	private boolean mAutoStart = true;

	/** Number of detector 0, 1 or 2 */
	private static int sNbDetectorToRun;

	// Members for audio capture and detection
	/** Detection SDK wrapper and configuration **/
	private Vector<AudioDetector> mDetectors = null;
	private Vector<AudioDetector.DetectorConfiguration> mDetectorConfigs = null;

	/** Audio Capture Thread **/
	private AudioCapture mAudioCapture = null;

	/** Runtime permission requests **/
	private static final int REQUEST_STARTDETECTION_PERMISSIONS = 123;
	private static final int REQUEST_STARTRECORD_PERMISSIONS = 124;


	public KantarSyncNow()
	{
		//init verbose with default preference in XML default_values.xml
		sVerbose = getResources().getBoolean(R.bool.verbose);
		//init nbDetectorToRun with default preference in XML default_values.xml
		sNbDetectorToRun = getResources().getInteger(R.integer.nbDetectorToRun);
		//check if nb detector is 1 or 2
		if (sNbDetectorToRun<1 || sNbDetectorToRun>2){
			sNbDetectorToRun = 2;
		}

		mDetectors = new Vector<AudioDetector>(sNbDetectorToRun, 1);

		mDetectorConfigs = new Vector<AudioDetector.DetectorConfiguration>(sNbDetectorToRun, 1);
		mDetectorConfigs.add(new AudioDetector.DetectorConfiguration());
		mDetectorConfigs.add(new AudioDetector.DetectorConfiguration());
	}

	/**
	 * Run time permissions request (since API 23)
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
	public boolean pushAudioBuffer(byte audioSampleBuffer[], int audioSampleBufferSize){
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

			Log.d("Kantar Printer",msg.toString())
		}
	}
}
