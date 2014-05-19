package com.trebisky.atopo;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

// A note on map scales.
// for the 24K series at least, a pixel scale of 112 dots per inch
//   will match the original scale on paper.
// The Xoom, with close to 150 dpi will be somewhat, although
//   not drastically denser.

public class MainActivity extends Activity implements LocationListener {

	private boolean gps_running;
	private boolean gps_first;

    // private final short LEVEL_START = Level.L_24K;
    // private final short LEVEL_START = Level.L_100K;
    // private final short LEVEL_START = Level.L_500K;
    // private final short LEVEL_START = Level.L_ATLAS;
    // private final short LEVEL_START = Level.L_STATE;
	
	// Tucson, Arizona
	private final double LONG_START = -110.94;
	private final double LAT_START = 32.27;
	private final short LEVEL_START = Level.L_ATLAS;
	
	// Joe Marty, Salt Lake City
	//private final double LONG_START = -111.7529;
	//private final double LAT_START = 40.7729;
	//private final short LEVEL_START = Level.L_ATLAS;
	
	// Grand Canyon, Tower of Ra
	// private final double LONG_START = -112.203;
	// private final double LAT_START = 36.141;
	
	// Grand Canyon, Horseshoe Mesa, top of peak
	//private final double LONG_START = -111.975;
	//private final double LAT_START = 36.024;
	
	// Grand Canyon, Horseshoe Mesa, east side (right)
	// private final double LONG_START = -111.97126;
	// private final double LAT_START = 36.02472;
	
	// Grand Canyon, Kelly Point Shivwitts
	// private final double LONG_START = -113.469;
	// private final double LAT_START = 35.835;
	
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
	
	
	// private final String tpq_dir = "/storage/sdcard1/topo/l5";
	// private final String tpq_file = "/storage/sdcard1/topo/l5/n36112b2.tpq";
	
	// Tablets and Phones look best with different default zoom factors.
	// My Samsung Galaxy S4 has a 5 inch 1920x1080 display (441 ppi)
	// Joes Samsung Galaxy Note 3 has a 5.7 inch 1920x1080 (386 ppi)
	//
	// My Xoom tablet has a 10.1 inch 1280x800 display (149 ppi)
	
	// 5-14-2014 set this to 1.0 for tablet, 2.0 for phone
	// private final double default_zoom = 1.0;
	private final double default_zoom = 2.0;
	
	private LocationManager locationManager;
	
	private final int gps_delay = 10 * 1000;
	
	private final int timer_delay = 250;
	
	private MyView view;

	private static Handler handle = new Handler();
	
	// This method was added 2-23-2014 -- First and foremost
	// it takes into account different naming of the external
	// SD card on different devices.  The first device I worked
	// with (the Motorola Xoom) did (and still does) call the
	// external slot "sdcard1", all other devices I have had
	// access to call it "extSdCard".
	// Note that I also check for maps that might be present
	// on the internal storage (universally called "sdcard0"),
	// but only after the check for maps on an external device
	// has failed.
	//
	// This means that if there are maps in both locations,
	// maps on an external card will hide maps on an internal card.
	// For the time being, that is just how it is.
	//
	// XXX - Someday it might be nice to "merge" maps from both locations,
	// but that will take some new coding in other parts of the
	// software.
	//
	// private final String file_base = "/storage/sdcard1/topo";
		
	private String find_files () {
		
		File f;
		
		f = new File ( "/storage/sdcard1/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/storage/sdcard1/topo";
		}

		f = new File ( "/storage/extSdCard/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/storage/extSdCard/topo";
		}

		f = new File ( "/storage/sdcard0/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/storage/sdcard0/topo";
		}
		return null;
	}
	
	// This function exists because we
	// cannot make the invalidate call
	// to trigger a redraw of the view
	// anyplace but on the UI thread.
	// This is called from the timer thread.
	private void post_invalidate () {
		handle.post(new Runnable() {
			public void run() {
				// invalidate the view,
				// which forces an onDraw()
				view.invalidate();
			}
		});
	}
	
	// This just has to be run on the UI thread too.
	public void post_gps_toggle () {
		handle.post(new Runnable() {
			public void run() {
				// invalidate the view,
				// which forces an onDraw()
				toggle_gps ();
			}
		});
	}
	
	private int marker_blink = 0;
	
	// Call this at 2 Hz.
	private void marker_tick () {
		if ( marker_blink == 0 )
			view.marker_type ( 1 );
		else
			if ( gps_running )
				view.marker_type ( 3 );
			else
				view.marker_type ( 2 );
		marker_blink = 1 - marker_blink;
	}
	
	private int marker_count = 0;

	// This runs in its own thread, so no UI updates here.
	// XXX we want to change the marker to indicate if the
	// GPS is active or not.
	class tickTask extends TimerTask {
		@Override
		public void run() {
			if ( ++marker_count % 2 == 0 ) {
				marker_tick ();
			}
			view.motion_tick ();
			post_invalidate ();
		}
	};
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		state.putDouble ( "long", Level.cur_long() );
		state.putDouble ( "lat", Level.cur_lat() );
		state.putBoolean ( "gps", gps_running );
		state.putShort ( "level", (short) Level.get_level() );
		super.onSaveInstanceState(state);
	}
	
	// XXX what about onRestoreInstanceState ???

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		double start_lat;
		double start_long;
		short start_level;
		String file_base;
		
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
		
		if ( savedInstanceState != null ) {
			start_lat = savedInstanceState.getDouble ( "lat" );
			start_long = savedInstanceState.getDouble ( "long" );
			gps_running = savedInstanceState.getBoolean ( "gps" );
			start_level = savedInstanceState.getShort ( "level" );
		} else {
			start_lat = LAT_START;
			start_long = LONG_START;
			start_level = LEVEL_START;
			gps_running = false;
		}
		
		if ( gps_running ) {
			start_gps ();
		}
		
		if ( default_zoom > 1.5 ) {
            MyView.set_hires ();
		}

		view = new MyView(this);
		
		file_base = find_files ();
		Level.setup ( file_base, start_long, start_lat, default_zoom );
		Level.set_level ( start_level );
		
		setContentView(view);

		Timer timer = new Timer();
		timer.schedule(new tickTask(), 0, timer_delay);

		// PowerManager powerManager = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "aTopo");
	}

	// We want an options menu attached to the menu button
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.atopo_menu, menu);
	    // MyView.onemsg ( "Menu" );
	    return true;
	}

	// simple code for menu selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.test:
	        //newGame();
	        return true;
	    //case R.id.help:
	        //showHelp();
	        //return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
    public void onPause() {
        super.onPause();

        if ( gps_running )
            locationManager.removeUpdates(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( gps_running )
        	start_gps ();
    }
    
    // Start out requesting updates as fast as possible,
    // when first update arrives, throttle back the rate.
    public void start_gps () {
    	MyView.Log ( "turning on GPS" );
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		gps_running = true;
		gps_first = true;
	    //MyView.onemsg ( "GPS on" );
    }
    
    public void stop_gps () {
    	MyView.Log ( "turning off GPS" );
        locationManager.removeUpdates(this);
		gps_running = false;
	    //MyView.onemsg ( "GPS off" );
    }
    
    public void toggle_gps () {
    	if ( gps_running )
    		stop_gps ();
    	else
    		start_gps ();
    }

	 public void onLocationChanged(Location loc) {

         double lat = loc.getLatitude();
         double lng = loc.getLongitude();
         // double alt = loc.getAltitude(); // in meters
         double accuracy = loc.getAccuracy(); // in meters
    	// MyView.Log ( "GPS accuracy: " + accuracy + " meters" );
    	// In one test I saw an initial accuracy of 45.6 meters,
    	// then all subsequent accuracies were 9.5 meters
    	
    	// worthless if nearly a kilometer
    	if ( accuracy > 750.0 ) return;
    	
    	if ( gps_first ) {
    		gps_first = false;
	        locationManager.removeUpdates(this);
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gps_delay, 0, this);
    	}
         
         Level.setpos ( lng, lat );
         
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
