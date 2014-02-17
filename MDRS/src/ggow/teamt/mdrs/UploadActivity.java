package ggow.teamt.mdrs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class UploadActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener{

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final String LOG_TAG = "MDRS - Upload";
	private GoogleMap mMap;
	private LinkedHashMap<Long, Location> locationTrail;
	private JSONArray metadata;
	private String MetaDataPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		// Show the Up button in the action bar.
		setupActionBar();

		/*	if(android.os.Build.VERSION.SDK_INT >= 19){
			Window w = getWindow();
			w.setFlags(
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		//	w.setFlags(
		//			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
		//			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		} else {
			Log.v(LOG_TAG, "Not KitKat+");
		}*/
		metadata = new JSONArray();
		getIntent();
		//locationTrail = intent.getParcelableExtra(RecordingActivity.TRAIL);
		locationTrail = RecordingActivity.locationTrail;
		System.out.println(locationTrail);
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
			try {
				upload();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(LOG_TAG,"Problem with upload method");
			}
			return true;
		case R.id.action_cancel:
			cancel();
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	private void cancel() {
		// TODO Add dialog and then send back to mapviewactivity

	}

	private void createJSONFromLocationTrail() {
		//Metadata gathered from user into JSON
		JSONObject titleObj = new JSONObject(); //Object at the start of the JSON which holds general info
		try {
			EditText etTitle = (EditText) findViewById(R.id.name);
			titleObj.put("title", etTitle.getText().toString());
			EditText etDesc = (EditText) findViewById(R.id.description);
			titleObj.put("description", etDesc.getText().toString());
			titleObj.put("startTime", locationTrail.entrySet().iterator().next().getKey());
			titleObj.put("endTime", getEndTime());
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		metadata.put(titleObj);
		Log.v(LOG_TAG, "Successful init metadata JSON");

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
			Log.v(LOG_TAG, "next location added to JSONArray");

		}
		metadata.put(locations);
	}

	private void saveMetadataToDevice() throws IOException{
		MetaDataPath = AudioRecordingService.AudioPath;
		Log.v(LOG_TAG, "current path for json is: " + MetaDataPath);
		MetaDataPath = MetaDataPath.substring(0, MetaDataPath.length()-9);
		Log.v(LOG_TAG,"after chopping the tail off it is: "+ MetaDataPath);
		MetaDataPath = MetaDataPath + "metadata.json";

		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			throw new IOException("SD Card is causing issues");
		}

		File directory = new File(MetaDataPath).getParentFile();
		if(!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Path to file could not be created");
		}

		try{
			FileWriter fw = new FileWriter(MetaDataPath);
			fw.write(metadata.toString());
			fw.close();
		} catch (IOException ioe) {
			Log.e(LOG_TAG, "Error saving metadata");
			ioe.printStackTrace();
		}
	}
	/*
	 * Gather together all the data that has been created and save it to a JSON ready
	 * for uploading when connecting to a computer.
	 * 
	 * In a future version this will be able to automatically upload to a 
	 * server but 
	 * for now it will just be old fashioned.
	 */
	private void upload() throws IOException {
		createJSONFromLocationTrail();
		saveMetadataToDevice();
		uploadToServer();
		Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
		startActivity(new Intent(this, MapViewActivity.class));
	}

	private void uploadToServer() {
		File audioFile = new File(AudioRecordingService.AudioPath);
		File metadataFile = new File (MetaDataPath);
		RequestParams params = new RequestParams();
		try {
			params.put("audio", audioFile);
			params.put("metadata", metadataFile);
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "Can't find a file to upload to server");
			e.printStackTrace();
		}
		httpUpload.post("mobile_upload", params, new AsyncHttpResponseHandler());
		//TODO Need some form of error checking in this. How do we know it has 
		//been successful? Also need to make it work in the background
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
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.uploadScreenMap))
					.getMap();			
			mMap.setMyLocationEnabled(false);
			mMap.getUiSettings().setCompassEnabled(false);
			mMap.getUiSettings().setMyLocationButtonEnabled(false);
			mMap.getUiSettings().setZoomControlsEnabled(false);
			fillMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				Log.v(LOG_TAG, "Map's all good brah");
			}
		}
	}

	private void fillMap(){
		boolean isFirstLocation = true;
		//Create line of recording
		Iterator<Location> it = locationTrail.values().iterator();
		PolylineOptions trail = new PolylineOptions();
		while(it.hasNext()){
			Location currentLocation = (Location) it.next();
			if (isFirstLocation){
				zoomInOnStart(currentLocation);
			}
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

	private void zoomInOnStart(Location start){
		CameraPosition cameraPosition = new CameraPosition.Builder()
		.target(new LatLng(start.getLatitude(), start.getLongitude()))
		.zoom(17)
		.build();
		mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
