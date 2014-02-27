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
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class UploadActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final String LOG_TAG = "MDRS - Upload";
	private GoogleMap mMap;
	private LinkedHashMap<Long, Location> locationTrail;
	private JSONArray metadata;
	private String metadataPath;
	private File images;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);

		// To show username when username/password support added
		// TextView text = (TextView) findViewById(R.id.logged_in_as);
		// text.append(username)

		// To show user image when login support added
		// ImageView userProfile = (ImageView) findViewById(R.id.user_image);
		// userProfile.setImageResource(getUserImage());

		fillGallery();

		// Show the Up button in the action bar.
		setupActionBar();

		/*
		 * if(android.os.Build.VERSION.SDK_INT >= 19){ Window w = getWindow();
		 * w.setFlags( WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
		 * WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION); //
		 * w.setFlags( // WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, //
		 * WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); } else {
		 * Log.v(LOG_TAG, "Not KitKat+"); }
		 */
		metadata = new JSONArray();
		getIntent();
		// locationTrail = intent.getParcelableExtra(RecordingActivity.TRAIL);
		locationTrail = RecordingActivity.getLocationTrail();
		Log.v(LOG_TAG, "locTrail from RecAct: " + locationTrail.toString());
		setUpMapIfNeeded();
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
				Log.e(LOG_TAG, "Problem with upload method");
			}
			return true;
		case R.id.action_cancel:
			cancel();
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback called when connected to GCore. Implementation of
	 * {@link ConnectionCallbacks}.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Callback called when disconnected from GCore. Implementation of
	 * {@link ConnectionCallbacks}.
	 */
	@Override
	public void onDisconnected() {
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		System.err.println("Connection failed");
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
			showDialog(connectionResult.getErrorCode());
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	private void cancel() {
		// TODO Add confirm dialog to move back to mapViewActivity

	}

	private void createJSONFromLocationTrail() {
		// Metadata gathered from user into JSON
		JSONObject titleObj = new JSONObject(); // Object at the start of the
		// JSON which holds general info
		try {
			EditText etTitle = (EditText) findViewById(R.id.title);
			titleObj.put("title", etTitle.getText().toString());
			EditText etDesc = (EditText) findViewById(R.id.desc);
			titleObj.put("description", etDesc.getText().toString());
			titleObj.put("startTime", RecordingActivity.getStartTime());
			titleObj.put("endTime", RecordingActivity.getEndTime());
		} catch (JSONException e1) {
			Log.e(LOG_TAG, "Failed to create JSON data");
			e1.printStackTrace();
		}
		metadata.put(titleObj);
		Log.v(LOG_TAG, "Successful init metadata JSON");

		// Location loading into the JSON
		JSONArray locations = new JSONArray(); // array to hold all location
		// objects
		Iterator<Location> it = locationTrail.values().iterator();
		while (it.hasNext()) {
			JSONObject obj = new JSONObject(); // Object to hold specific
			// location data
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

	private void saveMetadataToDevice() throws IOException {
		metadataPath = RecordingActivity.getCurrentRecordingPath();
		metadataPath = metadataPath + "/metadata.json";
		Log.v(LOG_TAG, "The metadata path is: " + metadataPath);

		// Checking directory (not sure if I need this. Will test and remove
		// if so.
		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			throw new IOException("SD Card is causing issues");
		}

		File directory = new File(metadataPath).getParentFile();
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Path to file could not be created");
		}

		try {
			FileWriter fw = new FileWriter(metadataPath);
			fw.write(metadata.toString());
			fw.close();
		} catch (IOException ioe) {
			Log.e(LOG_TAG, "Error saving metadata");
			ioe.printStackTrace();
		}
	}

	private boolean tarImagesUp() throws IOException {
		String archiveName = "images";
		File destination = new File(RecordingActivity.getCurrentRecordingPath());
		File source = new File(RecordingActivity.getCurrentRecordingPath()
				+ "/images");

		// should probably add some error checking to this...
		Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR,
				CompressionType.GZIP);
		images = archiver.create(archiveName, destination, source);

		if (images.canRead()) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Borrowed from Stack Overflow.
	 * 
	 * @user teedyay
	 * url:http://stackoverflow.com/questions/4943629/android-how-to
	 * -delete-a-whole-folder-and-content
	 */
	void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}

	/*
	 * Gather together all the data that has been created and save it to a JSON
	 * ready for uploading when connecting to a computer.
	 * 
	 * In a future version this will be able to automatically upload to a server
	 * but for now it will just be old fashioned.
	 */
	private void upload() throws IOException {
		createJSONFromLocationTrail();
		saveMetadataToDevice();

		if (tarImagesUp()) {
			deleteRecursive(new File(
					RecordingActivity.getCurrentRecordingPath() + "/images"));
		} else {
			Log.e(LOG_TAG, "Failed to zip images");
		}

		uploadToServer();
		Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
		startActivity(new Intent(this, MapViewActivity.class));
	}

	private void uploadToServer() {
		Log.v(LOG_TAG, "into uploadToServer()");

		mdrsHttpUpload client = new mdrsHttpUpload();

		// audio
		File audioFile = new File(RecordingActivity.getCurrentRecordingPath()
				+ "/audio.aac");
		Log.v(LOG_TAG, "Audio file: " + audioFile.toString());

		// metadata
		File metadataFile = new File(metadataPath);
		Log.v(LOG_TAG, "Metadata file: " + metadataFile.toString());

		// images
		File imagesFile = new File(RecordingActivity.getCurrentRecordingPath()
				+ "/images.tar.gz");
		Log.v(LOG_TAG,
				"Images file: " + RecordingActivity.getCurrentRecordingPath()
						+ "/images.tar.gz");

		RequestParams params = new RequestParams();
		try {
			params.put("audio", audioFile);
			params.put("metadata", metadataFile);
			params.put("images", imagesFile);
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "Can't find a file to upload to server");
			e.printStackTrace();
		}

		client.post("upload", params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				Log.v(LOG_TAG, "Successful upload.");
				// TODO add intent to move to map view activity from here
				// instead of out there
			}

			@Override
			public void onFailure(int statusCode,
					org.apache.http.Header[] headers, byte[] binaryData,
					java.lang.Throwable error) {
				Log.e(LOG_TAG, "Failed upload. Check server");
			}
		});
		Log.v(LOG_TAG, "Hopefully this should httpUpload");
		// TODO Need some form of error checking in this. How do we know it has
		// been successful? Also need to make it work in the background
	}

	/*
	 * Using a LinkedHashMap causes issues as unlike a list you cannot just get
	 * the end object or key. To work around this (and yes, this could be
	 * avoided with a better suited data structure) I take the key set and put
	 * it into a list. Then I take the end one from the list and return the Long
	 * value.
	 */
	private Long getEndTime() {
		List<Long> times = new ArrayList<Long>(locationTrail.keySet());
		return times.get(times.size() - 1);
	}

	private void setUpMapIfNeeded() { // would be used onResume I would assume
		Log.v(LOG_TAG, "into map setup");

		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.uploadScreenMap)).getMap();
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

	private void fillMap() {
		boolean isFirstLocation = true;
		// Create line of recording
		Iterator<Location> it = locationTrail.values().iterator();
		PolylineOptions trail = new PolylineOptions();
		while (it.hasNext()) {
			Location currentLocation = (Location) it.next();
			if (isFirstLocation) {
				zoomInOnStart(currentLocation);
			}
			trail.add(new LatLng(currentLocation.getLatitude(), currentLocation
					.getLongitude()));
		}
		mMap.addPolyline(trail);

		// End Marker
		mMap.addMarker(new MarkerOptions()
				.position(
						new LatLng(locationTrail.get(getEndTime())
								.getLatitude(), locationTrail.get(getEndTime())
								.getLongitude()))
				.draggable(false)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

		// Starter Marker
		mMap.addMarker(new MarkerOptions()
				// end marker
				.position(
						new LatLng(locationTrail.entrySet().iterator().next()
								.getValue().getLatitude(), locationTrail
								.entrySet().iterator().next().getValue()
								.getLongitude()))
				.draggable(false)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

		// TODO place markers where images are along the trail. Possibly MVC
		// with the horizontal scroll of them?
	}

	private void zoomInOnStart(Location start) {
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(start.getLatitude(), start.getLongitude()))
				.zoom(17).build();
		mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	private void fillGallery() {
		LinearLayout gallery = (LinearLayout) findViewById(R.id.gallery1);

		String imagesPath = RecordingActivity.getCurrentRecordingPath()
				+ "/images/";
		File imageDir = new File(imagesPath);
		File[] images = imageDir.listFiles();

		for (File file : images) {
			gallery.addView(insertPhoto(file.getAbsolutePath()));
		}
	}

	/*
	 * From http://android-er.blogspot.co.uk/2012/07/implement-gallery-like.html
	 */
	View insertPhoto(String path) {
		Bitmap bm = decodeSampledBitmapFromUri(path, 220, 220);

		LinearLayout layout = new LinearLayout(getApplicationContext());
		// layout.setLayoutParams(new LayoutParams(250, 250));
		layout.setGravity(Gravity.CENTER);

		ImageView imageView = new ImageView(getApplicationContext());
		imageView.setLayoutParams(new LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imageView.setImageBitmap(bm);

		layout.addView(imageView);
		return layout;
	}

	/*
	 * From http://android-er.blogspot.co.uk/2012/07/implement-gallery-like.html
	 */
	public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth,
			int reqHeight) {
		Bitmap bm = null;

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		bm = BitmapFactory.decodeFile(path, options);

		return bm;
	}

	/*
	 * From http://android-er.blogspot.co.uk/2012/07/implement-gallery-like.html
	 */
	public int calculateInSampleSize(

	BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}

		return inSampleSize;
	}
}
