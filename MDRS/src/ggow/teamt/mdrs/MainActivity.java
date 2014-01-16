package ggow.teamt.mdrs;

import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;



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
	private long elapsedTime = 0;
	private MediaRecorder recorder;
	ImageButton newRecordButton;

	public MainActivity() {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.3gp";
		System.err.println(mFileName);
		System.out.println("In the main bit of the thing. I think?");
	}

	//Maps stuff
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private TextView mMessageView;

	// These settings are the same as the settings for the map. They will in fact give you updates
	// at the maximal rates currently possible.
	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000)
			.setFastestInterval(16)
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	private void setUpMapIfNeeded() {
		if (mMap == null) {
			mMap= ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
					.getMap();

			if (mMap != null) {
				mMap.setMyLocationEnabled(true);
				mMap.setOnMyLocationButtonClickListener(this);
			}
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

	//NEED TO ADD TO THIS. REQWORK. THE MAP EXAMPLE IS PRETTY GOOD
	//FOR HELP
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
		mMessageView = (TextView) findViewById(R.id.message_text);
		addListenerOnButton();
	}

	private void addListenerOnButton() {
		newRecordButton = (ImageButton) findViewById(R.id.recImageButton);
		newRecordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					stopRecording();
					endTime = System.nanoTime();
					elapsedTime = endTime - startTime;
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
			
	}
	
	private void startRecording() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		recorder.setOutputFile(mFileName);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			recorder.prepare();
			recorder.start();
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
	public boolean onMyLocationButtonClick() {
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
}
