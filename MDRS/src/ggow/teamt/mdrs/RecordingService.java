package ggow.teamt.mdrs;

import java.util.LinkedHashMap;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

public class RecordingService extends Service {

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	private static final String LOG_TAG = "MDRS - RecordingService";
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final int MILLISECONDS_PER_SECOND = 1000;
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	private static final long UPDATE_INTERVAL =	MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;

	//Location stuff
	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private boolean mUpdatesRequested;
	private Editor mEditor;
	private SharedPreferences mPrefs;
	public static LinkedHashMap<Long, Location> locationTrail;

	public final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			//Do work here such as download a file
		}
	}

	@Override
	public void onCreate() {
		//Start the thread running the service.
		HandlerThread thread = new HandlerThread("ServiceStart",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show();
		//For each start request, send a message to start a job and deliver
		//the start ID so we know which request we're stopping when we finish
		//the job.
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		//Notification to keep in foreground
		NotificationCompat.Builder mBuilder = 
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("Recording")
		.setContentText("In Progress")
		.setOngoing(true);

		//Navigation Stack stuff
		//TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		//stackBuilder.addParentStack(arg0) TODO don't know what to put here



		locationConfiguration();
		audioRecordingConfiguration();

		return START_STICKY;
	}

	private void audioRecordingConfiguration() {
		// TODO Auto-generated method stub

	}

	private void locationConfiguration() {
		//Location Setup
		locationTrail = new LinkedHashMap<Long, Location>();
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL_IN_SECONDS);

		//Preferences stuff
		mPrefs = getSharedPreferences("SharedPreferences",
				Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		mEditor = mPrefs.edit();
		/*
		 * Create a new location client, using the enclosing class to
		 * handle callback.
		 */
		mLocationClient = new LocationClient(this, this, this);
		// Start with updates turned off
		mUpdatesRequested = true;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}



	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	}
}
