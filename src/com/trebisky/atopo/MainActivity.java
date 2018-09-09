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
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
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
	//private final double LONG_START = -110.94;
	//private final double LAT_START = 32.27;
	//private final short LEVEL_START = Level.L_ATLAS;
	
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
	// private double global_zoom = 1.0;
	// private double global_zoom = 2.0;
	
	private LocationManager locationManager;
	
	private int gps_delay = 1 * 1000;	// milliseconds

	private int gps_find_timeout = 0;		// seconds
	private int gps_find_repeat = 0;
	
	private final int timer_delay = 250;	// milliseconds
	private final int timer_hz = 4;
	
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
	// software.  Beside that, maps on an internal card never happen
	// at this point in time, maybe someday.
	//
	// private final String file_base = "/storage/sdcard1/topo";
	//
	// supposedly the right way to do this is:
	//   Environment.getExternalStorageDirectory().getAbsolutePath() + "/topo"
	// However, the path this gives me is /storage/emulated/0, which is
	//   complete useless nonsense.
	// Well, not quite nonsense, but by no means the path to the external SD card	

	private String file_runner ( String path ) {
		File d = new File ( path );
		if ( ! d.isDirectory() ) {
            // Log.e ( "Runner", "file: " + path );
            return null;
		}

		File[] files = d.listFiles();

		// We get both null here and arrays with zero length below
		// If we don't check for this, we end up referencing null pointers below
		if ( files == null ) {
           // Log.e ( "Runner", "dir: " + path + " is empty" );
           return null;
		}

        // Log.e ( "Runner", "dir: " + path + " " + files.length + " files" );

		for ( int i = 0; i < files.length; i++ ) {
			String name = files[i].getName();
			String xpath = path + "/" + name;
            File x = new File ( xpath );
            if ( x.isDirectory() ) {
            	if ( name.equals("topo") || name.equals("Topo") ) {
                    Log.e ( "Runner", "Aha!: " + path + " " + files.length + " files" );
                    return xpath;
                }
                String rv = file_runner ( xpath );
                if ( rv != null )
                	return rv;
            }
		}
		return null;
	}
	
	private String find_files () {
		
		File f;
		String rv;

		// String xx = Environment.getExternalStorageDirectory().getAbsolutePath();
		// Log.e ( "ENV", xx );

		/* XXX - special for testing */
//		f = new File ( "/sdcard/topo" );
//		if ( f.exists() && f.isDirectory() ) {
//            return "/sdcard/topo";
//		} else
		/* XXX - special for testing */

		rv = file_runner ( "/storage" );
		if ( rv != null ) {
            Log.e ( "Runner", rv  );
            return rv;
		}
		
		// The above should do the job, but if not, we try
		// some belt and suspenders using the historical code that follows.
		
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
		// For Mark Patton's "asian" Xoom
		f = new File ( "/mnt/external1/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/mnt/external1/topo";
		}
		// For the LG G-Pad  11-4-2016
		f = new File ( "/storage/external_SD/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/storage/external_SD/topo";
		}

		// For internal storage as per Paul  9-7-2018
		f = new File ( "/sdcard/topo" );
		if ( f.exists() && f.isDirectory() ) {
            return "/sdcard/topo";
		}
		return null;
	}
	
	private void trouble ( String msg ) {
			MyView.onemsg ( msg );
			// try to force an onDraw () event
			// view.invalidate();
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
	public void post_double_click () {
		handle.post(new Runnable() {
			public void run() {
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
	
	public boolean is_finding () {
		return gps_find_timeout > 0;
	}

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
        if ( gps_find_timeout > 0 ) {
			    gps_find_timeout--;
			    if ( gps_find_timeout <= 0 ) {
				stop_gps ();
			    }
			}
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
		int start_level;
		String file_base;
		boolean ok;
		double starting_zoom;

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
			start_level = (int) savedInstanceState.getShort ( "level" );
			starting_zoom = savedInstanceState.getDouble ( "zoom" );
		} else {
			// start_lat = LAT_START;
			// start_long = LONG_START;
			// start_level = LEVEL_START;
			// gps_running = false;

			Settings.init ( getSharedPreferences ( "atopo_preferences", Activity.MODE_PRIVATE ) );

			gps_running = Settings.get_gps ();;
			start_lat = Settings.get_start_lat ();
			start_long = Settings.get_start_long ();
			start_level = Settings.get_start_level ();
			starting_zoom = Settings.get_zoom ();
			MyView.set_display_mode ( Settings.get_display () );
		}
		
		if ( gps_running ) {
			start_gps ();
		}
		
		if ( starting_zoom > 1.5 ) {
		    MyView.set_hires ( true );
		}

		view = new MyView(this);
		
		file_base = find_files ();
		if ( file_base == null ) {
		    MyView.trouble ( "Cannot find map files" );
		    // The following is essential
		    setContentView(view);
		    return;
		}

		// MyView.onemsg ( "Start long: " + start_long );
		// MyView.onemsg ( "Start lat: " + start_lat );

		ok = Level.setup ( file_base, start_long, start_lat, start_level );
		if ( ! ok ) {
		    MyView.trouble ( "Cannot grog map files" );
		    // The following is essential
		    setContentView(view);
		    return;
		}

		Level.set_zoom ( starting_zoom );
		
		setContentView(view);

		Timer timer = new Timer();
		timer.schedule(new tickTask(), 0, timer_delay);

		if ( ! gps_running ) {
		    Level.set_alt_msg ( "GPS off" );
		}

		// PowerManager powerManager = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "aTopo");
	}

	// We want an options menu attached to the menu button
	// Note that although this works fine on Android 4.4 (KitKat)
	// with my Samsung Galaxy S4 phone, on my old Motorola Xoom
	// tablet, there is no dedicated menu button and thus no way
	// to get to these menus.  The answer is to set the minSDK to 8
	// and most importantly the target SDK to 10 in the AndroidManifest.xml
	// file, this yields a little menu icon at the bottom left and works fine.
	// Google really wants all this to be deprecated and for applications to
	// use the "Action Bar", but as long as this works, it saves screen
	// real estate and keeps me happy.  tjt  5-26-2014
	// They want me to set up my menu using XML also, foo on them.
	// This gets called only ONCE, when the menu is first brought up.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		/*
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.atopo_menu, menu);
	    */
		Settings.createMenu ( this, menu, getMenuInflater() );
	    // MyView.onemsg ( "Menu" );
	    return true;
	}

	// Unlike the above, this gets called every time.
	// However the intent is that it modify what the above set up.
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		Settings.tweakMenu ( this, menu );
		return true;
	}

	// simple code for menu selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if ( Settings.doMenu ( this, item ) ) {
            return true;
		}

        return super.onOptionsItemSelected(item);

	}

	@Override
    public void onPause() {
        super.onPause();
        
        Settings.set_start_lll ( Level.cur_long(), Level.cur_lat(), Level.get_level() );

        if ( gps_running )
            locationManager.removeUpdates(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( gps_running )
        	start_gps ();
    }

    /* 9-7-2018
     * Adding the save of current location to onStop and onDestroy works to
     * make Atopo come up at the last place viewed and makes the starting location
     * menu thing obsolete.
     */
    @Override
    public void onStop(){
        super.onStop();
        
        Settings.set_start_lll ( Level.cur_long(), Level.cur_lat(), Level.get_level() );
        Settings.save ();
    }

    @Override
    public void onDestroy(){
        super.onStop();
        
        Settings.set_start_lll ( Level.cur_long(), Level.cur_lat(), Level.get_level() );
        Settings.save ();
    }
    
    public void set_gps_delay ( int arg ) {
    	gps_delay = arg * 1000;
    }

    public void set_gps_find ( int timeout ) {
	gps_find_timeout = timeout * timer_hz;
	gps_find_repeat = 3;	// after 3 locations, turn off GPS
    }

    public boolean is_gps_running () {
	return gps_running;
    }
    
    // Start out requesting updates as fast as possible,
    // when first update arrives, throttle back the rate.
    public void start_gps () {
    	MyView.Log ( "turning on GPS" );
		Settings.set_gps ( true );
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		gps_running = true;
		gps_first = true;
        Level.set_alt_msg ( "GPS starting" );
	    //MyView.onemsg ( "GPS on" );
    }
    
    public void stop_gps () {
    	MyView.Log ( "turning off GPS" );
		Settings.set_gps ( false );
        locationManager.removeUpdates(this);
		gps_running = false;
	    //MyView.onemsg ( "GPS off" );
        Level.set_alt_msg ( "GPS off" );
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
         double alt = loc.getAltitude() * 3.28084; // convert to feet

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
         
         Level.setgps ( lng, lat, alt );

	if ( gps_find_repeat > 0 ) {
	    gps_find_repeat--;
	    if ( gps_find_repeat <= 0 ) {
		stop_gps ();
		gps_find_timeout = 0;
	    }
	}
         
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
