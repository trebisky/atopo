package com.trebisky.atopo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Settings {// We want an options menu attached to the menu button

	private static SharedPreferences myPref;
	
	static double pref_zoom;
	static double pref_start_long;
	static double pref_start_lat;
	static int pref_start_level;
	static int pref_display;
	static boolean pref_gps;
	
	/* I am really abusing these.
	 *  What I really want is buttons, but this will do.
	 */
	static MenuItem find_item;
	static MenuItem track_item;
	static MenuItem gps_item;
	
	// Tucson, Arizona
	// private static final double tuc_long = -110.94;
	// private static final double tuc_lat = 32.27;
	
	// Joe Marty, Salt Lake City
	// private static final double slc_long = -111.7529;
	// private static final double slc_lat = 40.7729;

	// Reno, Nevada
	// private static final double reno_long = -119.78294;
	// private static final double reno_lat = 39.47361;

	// Mt. Rainier, Washington
	private static final double rainier_long = -121.765;
	private static final double rainier_lat = 46.854;

	// public void Settings () { }
	public static void init ( SharedPreferences pref ) {
		myPref = pref;
		pref_zoom = myPref.getFloat ( "zoom", 2.0F );
		pref_start_long = myPref.getFloat ( "start_long", (float) rainier_long );
		pref_start_lat = myPref.getFloat ( "start_lat", (float) rainier_lat );
		pref_start_level = Level.decode_level ( myPref.getString ( "start_level", Level.encode_level(Level.L_ATLAS) ) );
		pref_display = myPref.getInt ( "display", 1 );
		pref_gps = myPref.getBoolean ( "gps", false );
	}
	
	public static void save () {
        SharedPreferences.Editor editor = myPref.edit();
        
        editor.putFloat ( "zoom", (float) pref_zoom );
        editor.putFloat ( "start_long", (float) pref_start_long );
        editor.putFloat ( "start_lat", (float) pref_start_lat );
        editor.putString ( "start_level", Level.encode_level(pref_start_level) );
        editor.putInt ( "display", pref_display );
        editor.putBoolean ( "gps", pref_gps );

        editor.apply();
        //if ( editor.commit() )
        //	Level.set_alt_msg ( "OK" );
        //else
        //	Level.set_alt_msg ( "FAIL" );
	}
	
	// Group codes -- used below
	public static final int G_ZOOM = 0;
	public static final int G_GPS = 1;
	public static final int G_SGPS = 2;
	public static final int G_LOC = 3;
	public static final int G_STAT = 4;

	// Item codes -- used below
	public static final int ZOOM = 1;

	public static final int GPS_TRACK = 11;
//	public static final int GPS_1 = 11;
// 	public static final int GPS_5 = 12;
// 	public static final int GPS_10 = 13;
	public static final int GPS_OFF = 14;
	public static final int GPS_FIND = 15;

	// public static final int LOC_TUC = 21;
	// public static final int LOC_SLC = 22;
	// public static final int LOC_RENO = 23;

	public static final int STAT_OFF = 31;
	public static final int STAT_DMS = 32;
	public static final int STAT_FRAC = 33;
	
	// Android documents recommend designing your menu layout in
	// an external resource file, then inflating it.  The problem is
	// that writing the XML is a pain and the GUI they provide to do it
	// is more or less undocumented.  Not only that, but you have to
	// bounce around between the XML file and this code.  I like it
	// better with all the code right here.

	public static boolean get_gps () {
        return pref_gps;
	}

	// This gets called when user interface turns GPS on or off
	// Thusly the GPS state gets saved when the app shuts down.
	// new behavior as of 12-26-2017
	public static void set_gps ( boolean val) {
        pref_gps = val;
	}

	public static double get_start_long () {
        return pref_start_long;
	}

	public static double get_start_lat () {
        return pref_start_lat;
	}

	public static int get_start_level () {
        return pref_start_level;
	}

	// Set long, lat, and level.
	// This allows startup at last location.
	// new behavior as of 12-26-2017
	public static void set_start_lll ( double a_long, double a_lat, int a_level) {
        pref_start_long = a_long;
        pref_start_lat = a_lat;
        pref_start_level = a_level;
	}
	
	public static double get_zoom () {
        return pref_zoom;
	}

	public static int get_display () {
        return pref_display;
	}

	public static void tweakMenu ( MainActivity app, Menu menu ) {
		boolean running = app.is_gps_running();
		boolean finding = false;
		boolean tracking = false;

        gps_item.setVisible(running);

        if ( running ) {
        	if ( app.is_finding() )
                finding = true;
        	else
                tracking = true;
        }
        find_item.setChecked(finding);
        track_item.setChecked(tracking);
	}

	public static void createMenu ( MainActivity app, Menu menu, MenuInflater inf ) {
		MenuItem m;

		m = menu.add ( G_ZOOM, ZOOM,  Menu.NONE, "Zoom 2.0" );
		m.setCheckable(true);
		if ( pref_zoom > 1.5 )
            m.setChecked(true);

		/* New 9-8-2018 */
		find_item = menu.add ( G_GPS, GPS_FIND, Menu.NONE, "GPS find location" );

		/* This was "GPS 1 second" up to 9-8-2018 */
		track_item = menu.add ( G_GPS, GPS_TRACK, Menu.NONE, "GPS track location" );
		/*
		menu.add ( G_GPS, GPS_1, Menu.NONE, "GPS 1 second" );
		menu.add ( G_GPS, GPS_5, Menu.NONE, "GPS 5 second" );
		menu.add ( G_GPS, GPS_10, Menu.NONE, "GPS 10 second" );
		*/
		// This makes these radio buttons.
		// menu.setGroupCheckable ( G_GPS, true, true );
		find_item.setCheckable ( true );
		track_item.setCheckable ( true );

        // item = menu.add ( G_SGPS, GPS_OFF, Menu.NONE, "GPS off" ).setChecked(true);
        gps_item = menu.add ( G_SGPS, GPS_OFF, Menu.NONE, "GPS off" );
		// menu.setGroupCheckable ( G_SGPS, true, true );
		gps_item.setCheckable ( true );
        gps_item.setChecked(true);

		/*
		m = menu.add ( G_LOC, LOC_TUC, Menu.NONE, "Start at Tucson" );
        if ( pref_start_lat < 35.0 )
            m.setChecked(true);

		m = menu.add ( G_LOC, LOC_SLC, Menu.NONE, "Start at Salt Lake" );
        if ( pref_start_lat > 40.0 )
            m.setChecked(true);

		m = menu.add ( G_LOC, LOC_RENO, Menu.NONE, "Start at Reno" );
        if ( pref_start_lat > 35.0 && pref_start_lat < 40.0 )
            m.setChecked(true);
		menu.setGroupCheckable ( G_LOC, true, true );
		 */

		m = menu.add ( G_STAT, STAT_OFF, Menu.NONE, "No long/lat display" );
        if ( pref_display == 0 )
            m.setChecked(true);
		m = menu.add ( G_STAT, STAT_DMS, Menu.NONE, "long/lat as DMS" );
        if ( pref_display == 1 )
            m.setChecked(true);
		m = menu.add ( G_STAT, STAT_FRAC, Menu.NONE, "long/lat as .XXX" );
        if ( pref_display == 2 )
            m.setChecked(true);
		menu.setGroupCheckable ( G_STAT, true, true );
	}
	
	public static boolean doMenu ( MainActivity app, MenuItem item ) {
		// Handle item selection
	    switch (item.getItemId()) {
	    case ZOOM:
	        // toggle checked indication
	    	if (item.isChecked()) {
	    		item.setChecked(false);
	    		pref_zoom = 1.0;
	    		save();
	    		Level.set_zoom ( 1.0 );
            	MyView.set_hires ( false );
	    	} else {
            	item.setChecked(true);
	    		pref_zoom = 2.0;
	    		save();
	    		Level.set_zoom ( 2.0 );
            	MyView.set_hires ( true );
	    	}
            //Level.set_alt_msg ( "Menu test sel" );
	        return true;

        // radio buttons for GPS state follow
	    case GPS_TRACK:
            item.setChecked(true);
            app.set_gps_delay ( 1 );
            app.start_gps();
            //MyView.Log ( "Menu gps 1 second track" );
	        return true;

	    // New 9-8-2018
	    case GPS_FIND:
            item.setChecked(true);
		    app.set_gps_delay ( 1 );
		    app.set_gps_find ( 60 );
		    app.start_gps();
            //MyView.Log ( "Menu gps find" );
	        return true;
/*
	    case GPS_5:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	app.set_gps_delay ( 5 );
	    		app.start_gps();
	    	}
	        return true;
	    case GPS_10:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	app.set_gps_delay ( 10 );
	    		app.start_gps();
	    	}
	        return true;
*/
	    case GPS_OFF:
            item.setChecked(false);
            app.stop_gps();
            //MyView.Log ( "Menu gps off" );
	        return true;

        // radio buttons for Starting Location
        /*
	    case LOC_TUC:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
                pref_start_long = tuc_long;
                pref_start_lat = tuc_lat;
                save();
	    	}
	        return true;
	    case LOC_SLC:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
                pref_start_long = slc_long;
                pref_start_lat = slc_lat;
                save();
	    	}
	        return true;
	    case LOC_RENO:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
                pref_start_long = reno_long;
                pref_start_lat = reno_lat;
                save();
	    	}
	        return true;
         */

        // radio buttons for Long/Lat display
	    case STAT_OFF:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	pref_display = 0;
                save();
                MyView.set_display_mode ( pref_display );
	    	}
	        return true;
	    case STAT_DMS:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	pref_display = 1;
                save();
                MyView.set_display_mode ( pref_display );
	    	}
	        return true;
	    case STAT_FRAC:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	pref_display = 2;
                save();
                MyView.set_display_mode ( pref_display );
	    	}
	        return true;

        default:
	        return false;
	}
}

} // THE END
