package ggow.teamt.mdrs;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.MapFragment;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MapViewActivity extends FragmentActivity
implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener,
OnMyLocationButtonClickListener{

	private final static int
	CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final String LOG_TAG = "MDRS - MapView";
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	public Location mCurrentLocation = null;
	public final static String START_LOCATION = "ggow.teamt.mdrs.location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(LOG_TAG, "into onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_view);

		if(android.os.Build.VERSION.SDK_INT >= 19){
			Window w = getWindow();
			w.setFlags(
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		//	w.setFlags(
		//			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
		//			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		} else {
			Log.v(LOG_TAG, "Not KitKat+");
		}
		
		setUpMapIfNeeded();
		isLocationEnabled(this);
		mLocationClient = new LocationClient(this, this, this);
		mLocationClient.connect();
		Log.v(LOG_TAG, "made it to the end of onCreate");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(LOG_TAG, "into onStart");

		// Connect the client.
		mLocationClient.connect();
	}

	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_settings:
			openSettings();
			return true;
		case R.id.action_feedback:
			feedback();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void startRecording(View view){
		if(mCurrentLocation == null) {
			Toast.makeText(this, "Still waiting on location.", Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = new Intent(this, RecordingActivity.class);
		intent.putExtra(START_LOCATION, mCurrentLocation);
		startActivity(intent);
	}

	private void setUpMapIfNeeded() { //would be used onResume I would assume
		Log.v(LOG_TAG, "Setting up map");

		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
					.getMap();
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setCompassEnabled(false);
			mMap.getUiSettings().setMyLocationButtonEnabled(false);
			mMap.getUiSettings().setZoomControlsEnabled(false);
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				Log.v(LOG_TAG, "Map successfully configured");
			}
		}
	}

	private void feedback(){
		final Intent email = new Intent(android.content.Intent.ACTION_SEND);
		email.setType("plain/text");
		email.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"ally.pcgf+mdrsfeedback@gmail.com"});
		email.putExtra(android.content.Intent.EXTRA_SUBJECT, "MDRS - Android App Feedback");
		startActivity(email);

	}

	@Override
	public boolean onMyLocationButtonClick() {
		System.err.println("in onMyLocationButtonClick()");
		return false;
	}

	@Override
	public void onLocationChanged(Location arg0) {
		System.err.println("in onLocChanged");
	}

	/**
	 * Callback called when connected to GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();
		mCurrentLocation = mLocationClient.getLastLocation();
		zoomInOnStart(mCurrentLocation);
	}

	private void zoomInOnStart(Location start){
		CameraPosition cameraPosition = new CameraPosition.Builder()
		.target(new LatLng(start.getLatitude(), start.getLongitude()))
		.zoom(15)
		.build();
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	/**
	 * Callback called when disconnected from GCore. Implementation of {@link ConnectionCallbacks}.
	 */
	@Override
	public void onDisconnected() {
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

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
	 * Handle results returned to the FragmentActivity
	 * by Google Play services
	 */
	@Override
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST :
			/*
			 * If the result code is Activity.RESULT_OK, try
			 * to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK :
				/*
				 * Try the request again
				 */
				break;
			}
		}
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

	@SuppressWarnings("unused")
	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode =
				GooglePlayServicesUtil.
				isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates",
					"Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Get the error code
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					resultCode,
					this,
					CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment =
						new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}

	private void isLocationEnabled(final Context context){
		LocationManager lm = null;
		boolean gps_enabled = false,network_enabled =false;
		if(lm==null)
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}catch(Exception ex){}
		try{
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}catch(Exception ex){}

		if(!gps_enabled && !network_enabled){
			Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
			dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					// TODO Auto-generated method stub
					Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
					context.startActivity(myIntent);
					//get gps
				}
			});
			dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					// TODO Auto-generated method stub

				}
			});
			dialog.show();

		}
	}
}