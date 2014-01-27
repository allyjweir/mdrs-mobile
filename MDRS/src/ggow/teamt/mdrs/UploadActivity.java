package ggow.teamt.mdrs;

import java.util.Iterator;
import java.util.LinkedHashMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class UploadActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener{

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final String LOG_TAG = "Upload - MDRS";
	private GoogleMap mMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent intent = getIntent();
		LinkedHashMap<Long, Location> locationTrail = intent.getParcelableExtra(RecordingActivity.TRAIL);
		setUpMapIfNeeded(locationTrail);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.upload, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_confirm:
			upload();
		case R.id.action_cancel:
			cancel();
			return true;
		
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void cancel() {
		// TODO Auto-generated method stub
		
	}

	private void upload() {
		// TODO Auto-generated method stub
		
	}

	private void setUpMapIfNeeded(LinkedHashMap<Long, Location> locationTrail) { //would be used onResume I would assume
		Log.v(LOG_TAG, "into SuMiN");

		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
					.getMap();
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setCompassEnabled(true);
			mMap.getUiSettings().setMyLocationButtonEnabled(true);
			fillMap(locationTrail);
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				Log.e(LOG_TAG, "Map's all good brah");
			}
		}
	}
	
	private void fillMap(LinkedHashMap<Long, Location> locationTrail){
		Iterator<Location> it = locationTrail.values().iterator();
		PolylineOptions trail = new PolylineOptions();
		while(it.hasNext()){
			Location currentLocation = (Location) it.next();
			trail.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
		}
		mMap.addPolyline(trail);
		//TODO	place markers where images are along the trail. Possibly MVC with the horizontal scroll of them?
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		System.err.println("Connection failed");
		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(
						this,
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
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
			showDialog(connectionResult.getErrorCode());
		}
	}

	/**
	 * Callback called when connected to GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Callback called when disconnected from GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onDisconnected() {
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}
}
