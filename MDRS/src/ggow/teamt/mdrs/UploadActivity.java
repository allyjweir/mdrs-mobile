package ggow.teamt.mdrs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
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
	private LinkedHashMap<Long, Location> locationTrail;
	private JSONArray metadata;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		// Show the Up button in the action bar.
		setupActionBar();
		metadata = new JSONArray();
		Intent intent = getIntent();
		locationTrail = intent.getParcelableExtra(RecordingActivity.TRAIL);
		setUpMapIfNeeded();
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

	/*
	 * Gather together all the data that has been created and save it to a JSON ready
	 * for uploading when connecting to a computer.
	 * 
	 * In a future version this will be able to automatically upload to a server but 
	 * for now it will just be old fashioned.
	 */
	private void upload() {
		//Metadata gathered from user into JSON
		JSONObject titleObj = new JSONObject(); //Object at the start of the JSON which holds general info
		try {
			titleObj.put("title", R.id.name);
			titleObj.put("description", R.id.description);
			titleObj.put("startTime", locationTrail.entrySet().iterator().next().getKey());
			titleObj.put("endTime", getEndTime());
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		metadata.put(titleObj);
		Log.e(LOG_TAG, "Successful init metadata JSON");
		
		//Location loading into the JSON
		JSONArray locations = new JSONArray(); //array to hold all location objects
		Iterator<Location> it = locationTrail.values().iterator();
		while (it.hasNext()){
			JSONObject obj = new JSONObject(); //Object to hold specific location data
			Location curLoc = (Location) it.next();
			try {
				obj.put("lon", curLoc.getLongitude());
				obj.put("time", curLoc.getTime());
				obj.put("lat", curLoc.getLatitude());
				obj.put("bearing", curLoc.getBearing());
				obj.put("altitude", curLoc.getAltitude());
				obj.put("speed", curLoc.getSpeed());
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			locations.put(obj);
			Log.e(LOG_TAG, "next location added to JSONArray");

		}
		metadata.put(locations);
		try{
			FileWriter file = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() 
				+ "/MDRS/" + "/" + RecordingActivity.folderTime + "/" + "metadata.json");
			file.write(metadata.toString());
			file.flush();
			file.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		Log.e(LOG_TAG, "Successful written metadata to storage.");

		//TODO HOW 
	}

	/*
	 * Using a LinkedHashMap causes issues as unlike a list you cannot just get the end
	 * object or key. To work around this (and yes, this could be avoided with a better
	 * suited data structure) I take the key set and put it into a list. Then I take the
	 * end one from the list and return the Long value.
	 */
	private Long getEndTime() {
		List<Long> times = new ArrayList<Long>(locationTrail.keySet());
		return times.get(times.size()-1);
	}

	private void setUpMapIfNeeded() { //would be used onResume I would assume
		Log.v(LOG_TAG, "into map setup");

		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
					.getMap();
			mMap.setMyLocationEnabled(true);
			mMap.getUiSettings().setCompassEnabled(true);
			mMap.getUiSettings().setMyLocationButtonEnabled(true);
			fillMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				Log.e(LOG_TAG, "Map's all good brah");
			}
		}
	}
	
	private void fillMap(){
		
		//Create line of recording
		Iterator<Location> it = locationTrail.values().iterator();
		PolylineOptions trail = new PolylineOptions();
		while(it.hasNext()){
			Location currentLocation = (Location) it.next();
			trail.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
		}
		mMap.addPolyline(trail);
		
		//Starter Marker
		mMap.addMarker(new MarkerOptions()
			.position(new LatLng(locationTrail.get(getEndTime()).getLatitude(), locationTrail.get(getEndTime()).getLongitude()))
			.draggable(false)
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
			);
		
		//End Marker
		mMap.addMarker(new MarkerOptions()//end marker
			.position(new LatLng(locationTrail.entrySet().iterator().next().getValue().getLatitude(), locationTrail.entrySet().iterator().next().getValue().getLongitude()))
			.draggable(false)
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
			);
			
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
