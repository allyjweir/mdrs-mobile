package ggow.teamt.mdrs;

import java.io.IOException;

import android.content.Intent;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends FragmentActivity
implements
ConnectionCallbacks,
OnConnectionFailedListener,
LocationListener,
OnMyLocationButtonClickListener {

	//Recording stuff
	private static final String LOG_TAG = "AudioRecordTest";
	private static String mFileName = "musak";
	boolean isRecording = false;
	private long startTime = 0;
	private long endTime = 0;
	private MediaRecorder recorder;
	ImageButton newRecordButton;

	//Maps stuff
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private TextView mMessageView;
	private Button myLocButton;
		
	public MainActivity() {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.3gp";
		System.err.println(mFileName);
		System.out.println("In the main bit of the thing. I think?");
	}

	//NEED TO ADD TO THIS. REQWORK. THE MAP EXAMPLE IS PRETTY GOOD
	//FOR HELP
	@Override
	public void onCreate(Bundle icicle) {
		Log.e(LOG_TAG, "Hello world");
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
		mMessageView = (TextView) findViewById(R.id.message_text);
		addListenerOnButton();
		setUpMapIfNeeded();
		setUpLocationClientIfNeeded();

	}
	

	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000)
			.setFastestInterval(16)
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	private void setUpMapIfNeeded() {
		if (mMap == null) {
			mMap= ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
					.getMap();
			Log.e(LOG_TAG, "map setup");
			mMap.setMyLocationEnabled(true);
			mMap.setOnMyLocationButtonClickListener(this);
			mMap.getUiSettings().setMyLocationButtonEnabled(true);
			mMap.getUiSettings().setZoomControlsEnabled(false);
			centerMapOnMyLocation();
		}
		else if (mMap != null) {
			Log.e(LOG_TAG, "map is all good brah");

		}
	}

	private void setUpLocationClientIfNeeded() {
		if (mLocationClient == null) {
			mLocationClient = new LocationClient (
					getApplicationContext(),
					this,  //ConnectionCallbacks
					this);  //OnConnectionFailedListener
		}
	}

	/**
	 * Button to get current Location. This demonstrates how to get the current Location as required
	 * without needing to register a LocationListener.
	 */
	public void showMyLocation(View view) {
		if (mLocationClient != null && mLocationClient.isConnected()) {
			String msg = "Location = " + mLocationClient.getLastLocation();
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		centerMapOnMyLocation();
		showMyLocation(getCurrentFocus());
		Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
		return false;
	}

	/**
	 * Implementation of {@link LocationListener}.
	 */
	@Override
	public void onLocationChanged(Location location) {
		mMessageView.setText("Location = " + location);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		System.err.println("Connection failed");
	}

	/**
	 * Callback called when connected to GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(REQUEST, this);

	}

	/**
	 * Callback called when disconnected from GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onDisconnected() {
		//Do nothing.
	}

	private void centerMapOnMyLocation() {
		Log.e(LOG_TAG, "ZONING IN ON THE TARGET");
		mMap.setMyLocationEnabled(true);
		Location location = mMap.getMyLocation();
		if (location != null) {
			LatLng myLocation = new LatLng(location.getLatitude(),
					location.getLongitude());
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,
					16));
		}

	}

	private void addListenerOnButton() {
		newRecordButton = (ImageButton) findViewById(R.id.recImageButton);
		newRecordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					stopRecording();
					endTime = System.nanoTime();
					Toast.makeText(MainActivity.this, "Stopping Recording", Toast.LENGTH_SHORT).show();
					isRecording = false;
					//NEED TO HANDLE STOP OF RECORDER HERE
				}
				else if (!isRecording) {
					startRecording();
					startTime = System.nanoTime();
					startRecording();
				}
				Toast.makeText(MainActivity.this, "RecButton is clicked!", Toast.LENGTH_SHORT).show();				
			}
		});

		myLocButton = (Button) findViewById(R.id.get_my_location_button);
		newRecordButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onMyLocationButtonClick();
			}
		});
	}

	private void startRecording() {
		recorder = new MediaRecorder();
		Log.e(LOG_TAG, "Hey pal, made that mediarecorder for you");
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		Log.e(LOG_TAG, "Hey pal, set the source and format for you");
		recorder.setOutputFile(mFileName);		
		Log.e(LOG_TAG, "Hey pal, set the file name for you");
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		Log.e(LOG_TAG, "Hey pal, set the audioencoder for you");

		try {
			recorder.prepare();
			Log.e(LOG_TAG, "Hey pal, prepared that for you");
			recorder.start();
			Log.e(LOG_TAG, "Hey pal, started that mediarecorder for you");
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() in startRecording failed");
		}
	}
	private void stopRecording() {
		try {
			recorder.stop();
			recorder.release();
			recorder = null;
		} catch (Exception e) {
			Log.e(LOG_TAG, "bloop");
		}
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    int itemId = item.getItemId();
		if (itemId == R.id.action_settings) {
			openSettings();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}



}
