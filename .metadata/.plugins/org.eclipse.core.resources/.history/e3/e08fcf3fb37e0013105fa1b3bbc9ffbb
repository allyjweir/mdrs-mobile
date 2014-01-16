package ggow.teamt.mdrs;

//THIS APPEARS TO BE MAJOR OVERKILL. TRYING TO SIMPLIFY BY USING IMAGEBUTTON MAIN CLASS.
//http://www.mkyong.com/android/android-imagebutton-example/

import java.io.IOException;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class RecordButton extends Button {
	boolean mStartRecording = true;
	private long startTime = 0;
	private long endTime = 0;
	private long elapsedTime = 0;
	private RecordButton mRecordButton = null;
	private MediaRecorder mRecorder = null;

	OnClickListener clicker = new OnClickListener() {
		@Override
		public void onClick(View v) {
			onRecord(mStartRecording);
			if (mStartRecording) {
				endTime = System.nanoTime();
				mRecordButton.setText("Stop recording");
			} else {
				startTime = System.nanoTime();
				mRecordButton.setText("Start recording");
				Context context = getApplicationContext();
				CharSequence text = "Recording saved. Length = "+((startTime-endTime)/1000000000)+" seconds";
				int duration = Toast.LENGTH_SHORT;
				Toast finishRecordToast = Toast.makeText(getApplicationContext(), text, duration);
				finishRecordToast.show();
			}
			mStartRecording = !mStartRecording;
		}
	};

	public RecordButton(Context ctx) {
		super(ctx);
		setText("Start recording");
		setOnClickListener(clicker);
	}
	
	public RecordButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		//Probs need something else in here
	}
	
	public  RecordButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//Probs need something else in here
	}
	
	private void onRecord(boolean start) {
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}
	
	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mFileName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
			mRecorder.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() in startRecording failed");
		}
	}
	private void stopRecording() {
		try {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
			mPlayButton.setEnabled(true);
		} catch (Exception e) {
			Log.e(LOG_TAG, "bloop");
		}
	}
}