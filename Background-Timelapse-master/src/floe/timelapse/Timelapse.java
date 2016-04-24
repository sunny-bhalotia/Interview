/****************************************************************\
*                Android Background Timelapse                    *
* Copyright (c) 2009-10 by Florian Echtler <floe@butterbrot.org> *
*  Licensed under GNU General Public License (GPL) 3 or later    *
\****************************************************************/

package floe.timelapse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.zip.GZIPInputStream;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.TextureView;
import android.view.View.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.text.format.Time;


public class Timelapse extends Activity {

	private TimelapseService mBoundService;
	private Intent myIntent;

	private TextureView tv;

	private ProgressDialog pd;
	private Handler ph = new Handler();

	private final String TAG = "Timelapse";

	@Override protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Watch for button clicks.
		Button button = (Button)findViewById( R.id.start );
		button.setOnClickListener(mBindListener);

		button = (Button)findViewById( R.id.stop );
		button.setOnClickListener(mUnbindListener);

		button = (Button)findViewById(R.id.convert);
		button.setOnClickListener(mConvertListener);

		button = (Button)findViewById(R.id.about);
		button.setOnClickListener(mAboutListener);

		// setup the preview surface
		tv = (TextureView)findViewById(R.id.view);

		// bind to the service, starting it when not yet running
		myIntent = new Intent( this, TimelapseService.class );
		bindService( myIntent, mConnection, Context.BIND_AUTO_CREATE );
	}

	@Override protected void onDestroy() {
		try {
			unbindService(mConnection);
		} catch (Exception e) { }
		super.onDestroy();
	}

	// catch & ignore config changes: screen rotation/keyboard slide
	// FIXME: this is a hack, the proper solution is MUCH more complicated - see:
	// https://github.com/commonsguy/cw-android/blob/master/Rotation/RotationAsync/src/com/commonsware/android/rotation/async/RotationAsync.java
	@Override public void onConfigurationChanged( Configuration newConfig ) {
		// just call the default method
		super.onConfigurationChanged( newConfig );
	}


	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((TimelapseService.TimelapseBinder)service).getService();
			//Toast.makeText( Timelapse.this, "Connected to timelapse service.", Toast.LENGTH_SHORT ).show();
			((TextView)findViewById( R.id.status )).setText( mBoundService.isRunning() ? "running" : "stopped" );
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(Timelapse.this, "Disconnected from timelapse service.", Toast.LENGTH_SHORT).show();
			((TextView)findViewById( R.id.status )).setText( "terminated" );
		}
	};


	// get delay value in ms from input element
	private int getDelay() {
		int mydelay = 1000;
		String delaytext = ((EditText)findViewById(R.id.delay)).getText().toString();
		try {
			mydelay = Integer.decode(delaytext);
		} catch (Exception e) { }
		if (mydelay < 1000) mydelay = 1000;
		return mydelay;
	}

	private OnClickListener mBindListener = new OnClickListener() {
		public void onClick(View v) {
			// mark the service as started - will not be
			// killed now, even if the connection is closed
			startService( myIntent );
			mBoundService.launch( tv, getDelay() );
			((TextView)findViewById( R.id.status )).setText( "running" );
		}
	};

	private OnClickListener mUnbindListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				// stop the service again - now will be
				// terminated once the connection closes.
				mBoundService.cleanup();
				stopService( myIntent );
				((TextView)findViewById( R.id.status )).setText( "stopped" );
			} catch (IllegalArgumentException e) { }
		}
	};

	private OnClickListener mConvertListener = new OnClickListener() {
		public void onClick( View v ) {
		}
	};

	private OnClickListener mAboutListener = new OnClickListener() {
		public void onClick( View v ) {
			String msg = "Background Timelapse 0.2\n(c) 2011 by floe@butterbrot.org\n\nContinously captures VGA images in the background for timelapse videos.\n\nStart: start the capture service\nStop: shut down the service\nConvert All: transform temporary images to PNGs (warning: slow!)\nAbout: this cruft\nDelay: milliseconds between captures";
			new AlertDialog.Builder( Timelapse.this )
				.setMessage(msg)
				.setPositiveButton( "Close", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { dialog.cancel(); }})
				.show();
		}
	};
}
