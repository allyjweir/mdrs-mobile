package ggow.teamt.mdrs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class RecordingActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	// General
	private static final String LOG_TAG = "MDRS - RecordingActivity";
	private String timeOfRecording;
	public static String currentRecordingPath;
	public static String imagesFolder;

	// Location
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final int MILLISECONDS_PER_SECOND = 1000;
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private boolean mUpdatesRequested;
	private Editor mEditor;
	private SharedPreferences mPrefs;
	public static LinkedHashMap<Long, Location> locationTrail;

	// Audio
	private MediaRecorder mRecorder;

	// Camera
	public FrameLayout preview;
	private Camera mCamera;
	private CameraPreview mPreview;
	private PictureCallback mPicture = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// Create a media file name
			//String timeStamp = String.valueOf(System.currentTimeMillis());
			//File pictureFile;
			File pictureFile = getOutputMediaFile();//new File(imagesFolder + "/IMG_" + timeStamp + ".jpg");

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(LOG_TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recording);
		try {
			buildDirectory();
		} catch (IOException e1) {
			Log.e(LOG_TAG, "Error in building Directories");
			e1.printStackTrace();
		}
		checkCameraHardware(this);

		// Translucent system bar - Still to figure out how to do top bar.
		// TODO interface tweaks
		if (android.os.Build.VERSION.SDK_INT >= 19) {
			Window w = getWindow();
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			// w.setFlags(
			// WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
			// WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		} else {
			Log.v(LOG_TAG, "Not KitKat+");
		}

		// Location Setup
		locationTrail = new LinkedHashMap<Long, Location>();
		Intent intent = getIntent();
		intent.getParcelableExtra(MapViewActivity.START_LOCATION);
		Toast.makeText(this, "Got starter location!", Toast.LENGTH_SHORT)
				.show();
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL_IN_SECONDS);

		// Open the shared preferences
		mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		mEditor = mPrefs.edit();
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callback.
		 */
		mLocationClient = new LocationClient(this, this, this);
		// Start with updates turned off
		mUpdatesRequested = true;

		// Audio Recording setup
		try {
			AudioRecordStart();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Camera Stuff
		mCamera = getCameraInstance();
		mPreview = new CameraPreview(this, mCamera);
		// Add CameraPreview to FrameLayout in activity_recording.xml
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		// Add a listener to the Capture button
		Button captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				TakePictureTask takePictureTask = new TakePictureTask();
				takePictureTask.execute();
				// mCamera.takePicture(null, null, mPicture);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(LOG_TAG, "initialising mLocationClient");
		mLocationClient.connect();
	}

	@Override
	protected void onPause() {
		Log.v(LOG_TAG, "Pausing application. muzer");
		releaseCamera(); // release the camera immediately on pause event
		Log.v(LOG_TAG, "Pausing application.");
		// Save the current setting for updates
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
		mEditor.commit();
		super.onPause();

	}

	@Override
	protected void onResume() {
		Log.v(LOG_TAG, "Resuming application");
		super.onResume();
		/*
		 * Get any previous setting for location updates Gets "false" if an
		 * error occurs
		 */
		if (mPrefs.contains("KEY_UPDATES_ON")) {
			mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);

			// Otherwise, turn off location updates
		} else {
			mEditor.putBoolean("KEY_UPDATES_ON", false);
			mEditor.commit();
		}
		// mCamera = getCameraInstance();
	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	protected void onStop() {
		Log.v(LOG_TAG, "Stopping application");
		// If the client is connected
		if (mLocationClient.isConnected()) {
			/*
			 * Remove location updates for a listener. The current Activity is
			 * the listener, so the argument is "this".
			 */
			// removeLocationUpdates(this);
			// TODO FIX THIS, WHATEVER IT DOES...
		}
		/*
		 * After disconnect() is called, the client is considered "dead".
		 */
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recording, menu);
		ActionBar actionBar = getActionBar();
		actionBar.hide();
		return true;
	}

	// Define the callback method that receives location updates
	@Override
	public void onLocationChanged(Location location) {
		// Report to the UI that the location was updated
		String msg = "Updated Location: "
				+ Double.toString(location.getLatitude()) + ","
				+ Double.toString(location.getLongitude());
		TextView text = (TextView) findViewById(R.id.current_location_ticker);
		text.setText(msg);
		Log.v(LOG_TAG, location.toString());
		locationTrail.put(location.getTime(), location);
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle dataBundle) {
		// Display the connection status
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		Location startLocation = mLocationClient.getLastLocation();
		Log.v(LOG_TAG, startLocation.toString());
		locationTrail.put(startLocation.getTime(), startLocation); // Possibly
		// remove
		// this
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	/*
	 * Called by Location Services if the connection to the location client
	 * drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services cancelled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			System.err
					.println("No resolution available. Some form of error with reconnect.");
			// showErrorDialog(connectionResult.getErrorCode());
		}
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.cancel_recording_message)
				.setTitle(R.string.cancel)
				.setPositiveButton(R.string.abandon,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								stopRecording();
								returnToMapView();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								return;
							}
						});
		AlertDialog dialog = builder.create();

	}

	public static LinkedHashMap<Long, Location> getLocationTrail() {
		return locationTrail;
	}

	// Broken out to its own class as it errors inside onBackPressed
	protected void returnToMapView() {
		startActivity(new Intent(this, MapViewActivity.class));
	}

	private void buildDirectory() throws IOException {
		Log.v(LOG_TAG, "Into buildDirectory()");
		// Initial construction
		timeOfRecording = String.valueOf(System.currentTimeMillis());
		currentRecordingPath = "MDRS/" + timeOfRecording;
		currentRecordingPath = sanitisePath(currentRecordingPath);
		if (!initDir(currentRecordingPath)) {
			Log.e(LOG_TAG, "Problem building main Dir.");
		} else {
			Log.v(LOG_TAG, "Initial path: " + currentRecordingPath);
		}

		// Build folder for images
		imagesFolder = getCurrentRecordingPath() + "/images";
		if (!initDir(imagesFolder)) {
			Log.e(LOG_TAG, "Problem building images dir");
		} else {
			Log.v(LOG_TAG, "Initial images path: " + imagesFolder);
		}

	}

	private String sanitisePath(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return Environment.getExternalStorageDirectory().getAbsolutePath()
				+ path;
	}
	
	private boolean initDir(String dir) throws IOException {
		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			throw new IOException("SD Card is causing issues");
		}

		File directory = new File(dir);//.getParentFile();
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Path to file could not be created");
		}
		return true;
	}

	public void AudioRecordStart() throws IOException {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mRecorder.setAudioEncodingBitRate(16);
		mRecorder.setAudioSamplingRate(44100);
		Log.v(LOG_TAG, "Audio path: " + getCurrentRecordingPath()
				+ "/audio.3gp");
		mRecorder.setOutputFile(getCurrentRecordingPath() + "/audio.3gp");
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() for recording failed");
		}
		mRecorder.start();
	}

	public void stopRecording() {
		Log.v(LOG_TAG, "stopRecording()");
		mRecorder.stop();
		mRecorder.reset();
		mRecorder.release();
		mRecorder = null;
		Log.v(LOG_TAG, "stopRecording() after recorder stuff");
		// releaseCamera();
	}

	// Made separate method to make stopRecording more universal for movement
	// around application
	public void moveToUpload(View view) {
		Log.v(LOG_TAG, "moveToUpload()");
		stopRecording();
		Log.v(LOG_TAG, "moveToUpload() after stopRecording");
		startActivity(new Intent(this, UploadActivity.class));
	}

	static String getCurrentRecordingPath() {
		return currentRecordingPath;
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	/*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK:
				/*
				 * Try the request again
				 */
				break;
			}
		}
	}

	@SuppressWarnings("unused")
	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Get the error code
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}

	// CAMERA STUFF

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			Log.v(LOG_TAG, "Camera hardware check success!");
			return true;
		} else {
			Log.e(LOG_TAG, "Camera hardware check fail!");
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
			Log.v(LOG_TAG, "Camera open success!");
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(LOG_TAG, "Camera not available");
			e.printStackTrace();

		}
		return c; // returns null if camera is unavailable
	}

	private void releaseCamera() {
		Log.v(LOG_TAG, "Into releaseCamera()");
		mCamera.stopPreview();
		mCamera.setPreviewCallback(null);
		// mCamera.release();
	}

	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;
		private static final String LOG_TAG = "MDRS - Camera Preview Class";

		public CameraPreview(Context context, Camera camera) {
			super(context);
			Log.v(LOG_TAG, "into CameraPreview constructor");
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			// mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.v(LOG_TAG, "into surfaceCreated()");
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.setDisplayOrientation(90);
				mCamera.startPreview();
			} catch (IOException e) {
				Log.d(LOG_TAG,
						"Error setting camera preview: " + e.getMessage());
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.v("LOG_TAG", "surfaceDestroyed()");
			// mCamera.setPreviewCallback(null);
			Log.v("LOG_TAG", "setPreviewCallback done");
			mCamera.stopPreview();
			Log.v("LOG_TAG", "stopPreview done");
			mCamera.setPreviewCallback(null);
			mCamera.release(); // release the camera for other applications
			mCamera = null;
			// mCamera = null;
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			Log.v(LOG_TAG, "into surfaceChanged()");
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// set preview size and make any resize, rotate or
			// reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
				Log.d(LOG_TAG,
						"Error starting camera preview: " + e.getMessage());
			}
		}
	}

	/** Create a File for saving the image */
	private File getOutputMediaFile() {

		File mediaStorageDir = new File(imagesFolder);

		// Create a media file name
		String timeStamp = String.valueOf(System.currentTimeMillis());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}

	/**
	 * A pretty basic example of an AsyncTask that takes the photo and then
	 * sleeps for a defined period of time before finishing. Upon finishing, it
	 * will restart the preview - Camera.startPreview().
	 */
	private class TakePictureTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			// This returns the preview back to the live camera feed
			Log.v(LOG_TAG, "Into onPostExecute()");
			mCamera.startPreview();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.v(LOG_TAG, "Into doInBackground()");
			mCamera.takePicture(null, null, mPicture);

			// Sleep for however long, you could store this in a variable and
			// have it updated by a menu item which the user selects.
			try {
				Log.v(LOG_TAG, "Having a snooze in doInBackground");
				Thread.sleep(3000); // 3 second preview
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, "Couldn't have a snooze!");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

	}

}