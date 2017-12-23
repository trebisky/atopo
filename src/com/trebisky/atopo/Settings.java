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
	
	// Tucson, Arizona
	private static final double tuc_long = -110.94;
	private static final double tuc_lat = 32.27;
	
	// Joe Marty, Salt Lake City
	private static final double slc_long = -111.7529;
	private static final double slc_lat = 40.7729;

	// Reno, Nevada
	private static final double reno_long = -119.78294;
	private static final double reno_lat = 39.47361;

	// public void Settings () { }
	public static void init ( SharedPreferences pref ) {
		myPref = pref;
		pref_zoom = myPref.getFloat ( "zoom", 2.0F );
		pref_start_long = myPref.getFloat ( "start_long", (float) reno_long );
		pref_start_lat = myPref.getFloat ( "start_lat", (float) reno_lat );
		pref_start_level = Level.decode_level ( myPref.getString ( "start_level", Level.encode_level(Level.L_ATLAS) ) );
		pref_display = myPref.getInt ( "display", 1 );
	}
	
	public static void save () {
        SharedPreferences.Editor editor = myPref.edit();
        
        editor.putFloat ( "zoom", (float) pref_zoom );
        editor.putFloat ( "start_long", (float) pref_start_long );
        editor.putFloat ( "start_lat", (float) pref_start_lat );
        editor.putString ( "start_level", Level.encode_level(pref_start_level) );
        editor.putInt ( "display", pref_display );

        editor.apply();
        //if ( editor.commit() )
        //	Level.set_alt_msg ( "OK" );
        //else
        //	Level.set_alt_msg ( "FAIL" );
	}
	
	// Group codes -- used below
	public static final int G_ZOOM = 0;
	public static final int G_GPS = 1;
	public static final int G_LOC = 2;
	public static final int G_STAT = 3;

	// Item codes -- used below
	public static final int ZOOM = 1;

	public static final int GPS_1 = 11;
	public static final int GPS_5 = 12;
	public static final int GPS_10 = 13;
	public static final int GPS_OFF = 14;

	public static final int LOC_TUC = 21;
	public static final int LOC_SLC = 22;
	public static final int LOC_RENO = 23;

	public static final int STAT_OFF = 31;
	public static final int STAT_DMS = 32;
	public static final int STAT_FRAC = 33;
	
	// Android documents recommend designing your menu layout in
	// an external resource file, then inflating it.  The problem is
	// that writing the XML is a pain and the GUI they provide to do it
	// is more or less undocumented.  Not only that, but you have to
	// bounce around between the XML file and this code.  I like it
	// better with all the code right here.
	
	public static double get_start_long () {
        return pref_start_long;
	}

	public static double get_start_lat () {
        return pref_start_lat;
	}

	public static int get_start_level () {
        return pref_start_level;
	}
	
	public static double get_zoom () {
        return pref_zoom;
	}

	public static int get_display () {
        return pref_display;
	}

	public static void createMenu ( Menu menu, MenuInflater inf ) {
		MenuItem m;

		m = menu.add ( G_ZOOM, ZOOM,  Menu.NONE, "Zoom 2.0" );
		m.setCheckable(true);
		if ( pref_zoom > 1.5 )
            m.setChecked(true);

		menu.add ( G_GPS, GPS_1, Menu.NONE, "GPS 1 second" );
		menu.add ( G_GPS, GPS_5, Menu.NONE, "GPS 5 second" );
		menu.add ( G_GPS, GPS_10, Menu.NONE, "GPS 10 second" );
		menu.add ( G_GPS, GPS_OFF, Menu.NONE, "GPS off" ).setChecked(true);
		menu.setGroupCheckable ( G_GPS, true, true );

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
	    case GPS_1:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
            	app.set_gps_delay ( 1 );
	    		app.start_gps();
	    	}
            //MyView.Log ( "Menu gps 1 second" );
	        return true;
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
	    case GPS_OFF:
	    	if ( ! item.isChecked() ) {
            	item.setChecked(true);
	    		app.stop_gps();
	    	}
            //MyView.Log ( "Menu gps off" );
	        return true;

        // radio buttons for Starting Location
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
