package com.trebisky.atopo;

import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

// A note on map scales.
// for the 24K series at least, a pixel scale of 112 dots per inch
//   will match the original scale on paper.
// The Xoom, with close to 150 dpi will be somewhat, although
//   not drastically denser.

public class MainActivity extends Activity implements LocationListener {

	private final boolean run_gps = false;
	// Tucson, Arizona
	// private final double LONG_START = -110.94;
	// private final double LAT_START = 32.27;
	
	// Grand Canyon, Tower of Ra
	// private final double LONG_START = -112.203;
	// private final double LAT_START = 36.141;
	
	// Grand Canyon, Horseshoe Mesa, top of peak
	//private final double LONG_START = -111.975;
	//private final double LAT_START = 36.024;
	
	// Grand Canyon, Horseshoe Mesa, east side (right)
	private final double LONG_START = -111.97126;
	private final double LAT_START = 36.02472;
	
	// Dead Center of Sonora Quad, California
	// private final double LONG_START = -120.438;
	// private final double LAT_START = 37.938;
	//private final double LONG_START = -120.50 +(7.5/60.0/2.0);
	//private final double LAT_START = 38.0 -(7.5/60.0/2.0);
	
	// private final int MAP_WIDTH = 1280;
	// private final int MAP_HEIGHT = 800;

	// Entire US at level 1  12x8 = 96 maplets
	// private final String tpq_file = "/storage/sdcard1/us1map1.tpq";
	// private final String tpq_file = "/storage/sdcard1/topo/us1map1.tpq";
	
	// Entire US at level 2  59x26 = 1534 maplets
	// private final String tpq_file = "/storage/sdcard1/us1map2.tpq";
	// private final String tpq_file = "/storage/sdcard1/topo/us1map2.tpq";
	
	// TODO - ultimately I would like to have this program look
	// on both sdcard0 and sdcard1 - this would allow a person who
	// did not have an SD card slot to put maps on internal storage.
	// Also I would want this program to "merge" both file collections
	// if a user had both, allowing them to have an internal set of files
	// optionally augmented by files on the SD card.
	private final String tpq_base = "/storage/sdcard1/topo";
	
	// private final String tpq_dir = "/storage/sdcard1/topo/l5";
	// private final String tpq_file = "/storage/sdcard1/topo/l5/n36112b2.tpq";
	
	private LocationManager locationManager;
	
	private final int gps_delay = 10 * 1000;
	
	private final int delay = 2 * 1000;
	
	private MyView view;
	private Level level; 
	private MyLocation location;

	private static Handler handle = new Handler();

	// This runs in its own thread, so no UI updates here.
	class tickTask extends TimerTask {
		@Override
		public void run() {
			//view.nextFile();

			// We invalidate the view, which forces an onDraw()
			handle.post(new Runnable() {
				public void run() {
					// view.invalidate();
				}
			});
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// boolean isPortrait = getResources().getConfiguration().orientation ==
		// Configuration.ORIENTATION_PORTRAIT;
		// int frameBufferWidth = isPortrait ? MAP_HEIGHT : MAP_WIDTH;
		// int frameBufferHeight = isPortrait ? MAP_WIDTH : MAP_HEIGHT;

		// Bitmap frameBuffer = Bitmap.createBitmap(frameBufferWidth,
		// frameBufferHeight, Config.RGB_565);

		// float scaleX = (float) frameBufferWidth
		// / getWindowManager().getDefaultDisplay().getWidth();
		// float scaleY = (float) frameBufferHeight
		// / getWindowManager().getDefaultDisplay().getHeight();
		
		// Fire up GPS
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if ( run_gps ) {
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        gps_delay, 0, this);
		}

		view = new MyView(this);
		
		level = new Level ( tpq_base );
		// level.set_24k ();
		// level.set_100k ();
		// level.set_500k ();
		// level.set_atlas ();
		level.set_state ();
		
		location = new MyLocation ( level );
		location.set (LONG_START,LAT_START);
		
		view.setup ( level, location );
		
		setContentView(view);

		//Timer timer = new Timer();
		//timer.schedule(new tickTask(), 0, delay);

		// PowerManager powerManager = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "aTopo");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public void onPause() {
            super.onPause();

            locationManager.removeUpdates(this);
    }

    @Override
    public void onResume() {
            super.onResume();

            if ( run_gps )
	            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            gps_delay, 0, this);
    }

	 public void onLocationChanged(Location loc) {

         double lat = loc.getLatitude();
         double lng = loc.getLongitude();
         
         location.set ( lng, lat );
         
         view.invalidate();
 }

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

}

// THE END
