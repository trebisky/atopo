package com.trebisky.atopo;

import java.io.File;

// Class to manage level specific data
public class Level {
	
	// Instance fields
	public String path;
	public String prefix;
	public int level; 
	public boolean valid;
	private int index;
		
	// Set only for state and atlas levels
	public String onefile;
		
	public double south;
	public double east;
		
	// overall size of maps in degrees
	public double map_long, map_lat;
		
	// number of maplets in a map file
	public int num_long, num_lat;
		
	// maplet size in degrees
	public double maplet_dlong, maplet_dlat;
		
	// maps per degree (for level).
	public int num_maps_long, num_maps_lat;
		
	// quads per map
	// a "quad" is a fixed 1/8 degree unit.
	public int quad_lat_count, quad_long_count;
		
	private MapletCache maplet_cache;
	
	public double base_zoom;
	public double zoom;
	
	// ------------ Static fields
	// ------------
	private static FileCache file_cache;
	
	// XX could perhaps change to a Java enum someday
	// Do NOT use these as array indices.
	public static final int L_STATE = 0;
	public static final int L_ATLAS = 1;
	public static final int L_500K = 2;
	public static final int L_100K = 3;
	public static final int L_24K = 4;
	public static final int L_24KZ = 5;
	
	/* These encode/decode strings are just used to save the
	 * current level in the settings storage.
	 */
	public static String encode_level ( int l ) {
		switch ( l ) {
		case L_STATE:	return "state";
		case L_ATLAS:	return "atlas";
		case L_500K:	return "500K";
		case L_100K:	return "100K";
		case L_24K:	    return "24K";
		case L_24KZ:	    return "24KZ";
		default: 		return "--";
		}
	}

	public static int decode_level ( String s ) {
		if ( s.equals("state") ) {
			return L_STATE;
		} else if ( s.equals("atlas") ) {
			return L_ATLAS;
		} else if ( s.equals("500K") ) {
			return L_500K;
		} else if ( s.equals("100K") ) {
			return L_100K;
		} else if ( s.equals("24K") ) {
			return L_24K;
		} else if ( s.equals("24KZ") ) {
			return L_24KZ;
		} else {
			return L_ATLAS;
		}
	}

	// private static final int MAX_LEVELS = 5;
	private static final int MAX_LEVELS = 6;
	private static Level levels[] = new Level[MAX_LEVELS];

	private static int last_level;
	
	private static Level cur_level;
	
	private static double cur_long;
	private static double cur_lat;
	private static double cur_alt;
	private static String alt_msg = null;
	
	// instance constructor
	
	public Level ( int l, String p, String extra, double z ) {
		level = l;
		path = p;
		base_zoom = z;
		zoom = z;
			
		if ( l == L_STATE || l == L_ATLAS ) {
			onefile = extra;
		} else {
			prefix = extra;
		}
			
		maplet_cache = new MapletCache ();
			
		valid = probe_map();
	}
	
	// allow zoom to be reset after startup.
	public static void set_zoom ( double z )
	{
		for ( int l=0; l <= last_level; l++ ) {
			levels[l].zoom = levels[l].base_zoom * z;
		}
	}
	
	// Called at startup during level setup
	// read some map file to get header info.
	private boolean probe_map () {
		String probe_map;
		tpqFile tpq;
			
		probe_map = map_to_probe();
		//MyView.Log( "Probing map: " + probe_map);
			
		if ( probe_map == null ) {
			MyView.Log ( "Probe fails for level " + level );
			// MyView.onemsg("Probe_fails for level " + level );
			return false;
		}
			
		// MyView.onemsg("Probing " + probe_map);
			
		tpq = file_cache.fetch(path,probe_map);
		
		if ( ! tpq.isvalid() ) {
			MyView.Log ( "Probe fails, bad TPQ: " + probe_map);
			// MyView.onemsg ( "Probe fails, bad TPQ: " + probe_map);
			return false;
		}
			
		// no need to keep this open after a probe.
		tpq.close();
		
		map_long = tpq.east() - tpq.west();
		map_lat = tpq.north() - tpq.south();
			
		// maplets per map
		num_long = tpq.num_long();
		num_lat = tpq.num_lat();
		//MyView.Log2( "Probing num long/lat: ", num_long, num_lat);
				
		// degrees per maplet
		maplet_dlong = map_long / num_long;
		maplet_dlat = map_lat / num_lat;
				
		// maps per degree
		// silly for atlas and state levels.
		// and we get a divide by zero too !!
		if ( map_long <= 1.02 ) {
			num_maps_long = (int) ( 1.0002 / map_long);
			num_maps_lat = (int) ( 1.0002 / map_lat);
			// number of 1/8 degree quads in a single map
			// really only needed for 100K series, but will
			// be accessed for all but state and atlas.
			quad_long_count = 8 / num_maps_long;
			quad_lat_count = 8 / num_maps_lat;
			//MyView.Log2( "Probing num_maps long/lat: ", num_maps_long, num_maps_lat);
		}
			
		// only used for state and altas levels
		south = tpq.south();
		east = tpq.east();
		return true;
	}
		
	// Called at startup during level setup
	private String map_to_probe () {
			
		if ( onefile != null ) {
			return onefile;
		}
			
		File f = new File ( path );
		
		// It bothers me to do this, given that there may
		// be 12,000 entries in the list and I only want the
		// first one, but I don't know how better to do this.
		// It goes on the stack and should evaporate,
		// and it happens only once at startup.
		// it does work just fine.
		File [] list = f.listFiles();	// may be huge !

		if ( list == null )
		    return null;
			
		for ( File m: list ) {
			String a_file = m.getName();
			
			if ( a_file == null )
				continue;
			
			if ( a_file.length() < 3 )
				continue;
				
			if ( a_file.substring(a_file.length()-3).equals("tpq") ) {
				// return path + "/" + a_file;
				return a_file.substring(0,a_file.length()-4);
			}
		}
		
		return null;
	}

	// gps calls this
	public static void setgps ( double _long, double _lat, double _alt ) {
		setpos_i ( _long, _lat );
		cur_alt = _alt;
		alt_msg = null;
	}

	// called at startup and when we jog position
	public static void setpos ( double _long, double _lat ) {
		setpos_i ( _long, _lat );
		if ( alt_msg == null )
            alt_msg = " ";
	}
	
	private static void setpos_i ( double _long, double _lat ) {
		
		// Before we allow the user to move to a new center location
		// we want to verify that a map is there (this means it will
		// never be possible to move a map edge or corner beyond center).
		if ( ! check_map ( _long, _lat ) ) {
			MyView.onemsg("No Map at x,y");
			return;
		}
				
		cur_long = _long;
		cur_lat = _lat;
	}
	
	// We want to display coordinates to the nearest foot
	// more or less. 
	// The earths radius is (average) 3957 miles
	// 1 degree is 69.063 miles
	// 1 arc-minute is 1.15 miles
	// 1 arc-second is 101.3 feet
	// .01 arc-second is 1.013 feet
	// .000001 degrees is 0.36 feet

	private static String dms ( double val ) {
		boolean neg = false;
		int d, m;
		
		if ( val < 0.0 ) {
			neg = true;
			val = -val;
		}
		d = (int) val;
		val = (val - d) * 60;
		m = (int) val;
		val = (val - m) * 60;

		if ( neg )
		    return String.format("-%d:%02d:%05.2f", d, m, val );
		else
		    return String.format("%d:%02d:%05.2f", d, m, val );
	}
	
	private static String d2f ( double val ) {
		return String.format("%12.6f", val );
	}

	public static String cur_long_f () {
		return d2f ( cur_long );
	}
	
	public static String cur_lat_f () {
		return d2f ( cur_lat );
	}

	public static String cur_long_dms () {
		return dms ( cur_long );
	}
	
	public static String cur_lat_dms () {
		return dms ( cur_lat );
	}
	
	public static double cur_long () {
		return cur_long;
	}
	
	public static double cur_lat () {
		return cur_lat;
	}
	
	public static void set_alt_msg ( String arg ) {
		alt_msg = arg;
	}

	// I don't think we ever use this
	public static double cur_alt () {
		return cur_alt;
	}

	public static String cur_alt_string () {
		if ( alt_msg != null )
			return alt_msg;
		else
            return String.format("%5.0f", cur_alt );
	}
	
	public static void jogpos ( double dlong, double dlat ) {
		double new_long = cur_long + dlong;
		double new_lat = cur_lat + dlat;
		
		if ( check_map ( new_long, new_lat ) ) {
			setpos ( new_long, new_lat );
		}
	}

	private static Level find_level ( int level ) {
		int i;

		for ( i=0; i<= last_level; i++ ) {
		    if ( levels[i].level == level )
			return levels[i];
		}
		return null;
	}
		
	// static methods
	public static boolean setup ( String base, double _long, double _lat, int _level ) {
		
		Level l;
		int lev = 0;

		file_cache = new FileCache ();
		
		l = new Level ( L_STATE, base, "us1map1", 1.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		l = new Level ( L_ATLAS, base, "us1map2", 1.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		l = new Level ( L_500K, base + "/l3", "g", 1.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		l = new Level ( L_100K, base + "/l4", "c", 1.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		l = new Level ( L_24K, base + "/l5", "n", 1.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		l = new Level ( L_24KZ, base + "/l5", "n", 2.0 );
		if ( l.valid ) {
		    l.index = lev;
		    levels[lev++] = l;
		}

		// MyView.onemsg ( "I see valid levels: " + lev );
		last_level = lev - 1;

		if ( last_level < 0 )
		    return false;

		/* XXX L_24K and L_24KZ can and should share maplet cache */

		l = find_level ( _level );
		if ( l != null ) {
		    cur_level = l;
		    // MyView.onemsg ( "Setup OK on level " + cur_level.level );
		} else {
		    cur_level = levels[last_level];
		    // MyView.onemsg ( "Setup Reset to level " + cur_level.level );
		}

		// set_level ( _level );
		
		/* XXX */
		if ( ! check_map ( _long, _lat ) ) {
		    _long = -100.0;
		    _lat = 35.0;
		}

		setpos ( _long, _lat );

		// MyView.onemsg ( "Cur long: " + cur_long );
		// MyView.onemsg ( "Cur lat: " + cur_lat );

		return true;
	}
	
	/* Zoom out (less detail) */
	public static boolean up () {
		MyView.Log ( "Try up from level " + cur_level.level );
		if ( cur_level.index <= 0 )
			return false;
		Level new_level = levels[cur_level.index-1];
		MyView.Log ( "Go up to level " + new_level.level );
		
		if ( new_level.probe() ) {
			cur_level = new_level;
			return true;
		}
		return false;
	}
	
	/* Zoom in (more detail) */
	public static boolean down () {
		MyView.Log ( "Try down from level " + cur_level.level );
		if ( cur_level.index >= last_level )
			return false;
		Level new_level = levels[cur_level.index+1];
		MyView.Log ( "Go down to level " + new_level.level );
		
		if ( new_level.probe() ) {
			cur_level = new_level;
			return true;
		}
		return false;
	}
	
	// instance method
	private boolean probe () {
		int wx = maplet_x(cur_long);
		int wy = maplet_y(cur_lat);
		Maplet m = maplet_lookup(wx,wy);
		return m != null;
	}
	
//	private static void set_level ( int arg ) {
//		if ( arg >= 0 && arg < NUM_LEVELS ) {
//			cur_level = levels[arg];
//			return;
//		}
//		// failsafe on initialization
//		if ( cur_level == null )
//			cur_level = levels[L_STATE];
//	}

	public static double get_zoom () {
		return cur_level.zoom;
	}
	
	
	public static int get_level () {
		return cur_level.level;
	}
	
	public static boolean cur_check_map () {
		return check_map ( cur_long, cur_lat );
	}
	
	// Called by location class to see if there
	// is a map under a new center location
	public static boolean check_map ( double _long, double _lat ) {
		String map;
		tpqFile tpq;
		
		// Figure out which map file the coordinates are in.
		// form is something like "n36112a1"
		map = encode_map ( _long, _lat );
		tpq = file_cache.fetch ( cur_level.path, map );
		if ( ! tpq.isvalid() ) {
			MyView.setmsg ( "TPQ invalid: " + map );
			return false;
		}
		
		if ( _long < tpq.west() || _long > tpq.east() || _lat < tpq.south() || _lat > tpq.north() ) {
			MyView.setmsg ( "New Location out of Bounds: " + map );
			MyView.setmsg ( " Lo/La: " + _long + " " + _lat );
			MyView.setmsg ( " W/E: " + tpq.west() + " " + tpq.east() );
			MyView.setmsg ( " S/N: " + tpq.south() + " " + tpq.north() );
			return false;
		}
		return true;
	}
	
	// Figure out which map file the coordinates are in.
	// form is "n36112a1.tpq"
	// Called when we only want to get into from the
	// map header.  Used in Location.check_map to verify
	// that maps exist under a given long, lat and in
	// MyView.onDraw to get header info for the center
	// maplet.
	public static String encode_map ( double _long, double _lat ) {
		return cur_level.encode_map_i ( cur_level.maplet_x(_long), cur_level.maplet_y(_lat) );
	}
	
	// Called by onDraw in view
	public static tpqFile cur_fetch_map ( String arg ) {
		return cur_level.fetch_map ( arg );
	}
	
	private tpqFile fetch_map ( String arg ) {
		return file_cache.fetch(path,arg);
	}
	
	public static Maplet cur_maplet_lookup ( int world_x, int world_y ) {
		return cur_level.maplet_lookup ( world_x, world_y );
	}
	
	// instance method--
	// Arguments are world maplet x,y
	// origin in the lower left corner.
	public Maplet maplet_lookup ( int world_x, int world_y ) {
		Maplet rv;
		tpqFile tpq;
		int sheet_x, sheet_y;
		int map_x, map_y;
		int idx;
		String name;
		
		//MyView.Log( "maplet_lookup: " + world_x + " " + world_y );
		rv = maplet_cache.fetch( world_x, world_y);
		if ( rv != null )
			return rv;
		
		// special case for state and atlas levels
		if ( onefile != null ) {
			//MyView.Log ( "maplet_lookup: " + onefile );
			tpq = fetch_map ( onefile );
			if ( ! tpq.isvalid() ) return null;
			//MyView.Log ( "maplet_lookup: tpq " + tpq );
			sheet_x = world_x;
			sheet_y = world_y;
			//MyView.Log2( "maplet_lookup: sheet_xy" , sheet_x, sheet_y);
		} else {
			name = encode_map_i ( world_x, world_y );
			//MyView.Log ( "maplet_lookup: " + name );
			tpq = fetch_map ( name );
			if ( ! tpq.isvalid() ) return null;
			//MyView.Log ( "maplet_lookup: tpq " + tpq );
			
			map_x = world_x / num_long;
			map_y = world_y / num_lat;
			//MyView.Log( "maplet_lookup: mx,my " + map_x + " " + map_y );
		
			sheet_x = world_x - map_x * num_long;
			sheet_y = world_y - map_y * num_lat;
			//MyView.Log2( "maplet_lookup: sheet_xy " , sheet_x, sheet_y);
		}
		
		//MyView.Log2( "maplet_lookup: num_xy" , num_long, num_lat );
		// world_x comes in counting right to left.
		// world_y comes in counting bottom to top.
		// sheet_x needs to count from left to right.
		// sheet_y needs to count from top to botom.
		sheet_x = num_long - sheet_x - 1;
		sheet_y = num_lat - sheet_y - 1;
		//MyView.Log2( "maplet_lookup: sheet_xy " , sheet_x, sheet_y);
		
		if ( sheet_x < 0 || sheet_x >= num_long ) { return null; }
		if ( sheet_y < 0 || sheet_y >= num_lat ) { return null; }
		
		idx = sheet_y * num_long + sheet_x;
		//MyView.Log( "maplet_lookup: idx " + idx);
		
		rv = maplet_cache.load( world_x, world_y, tpq, idx );
		return rv;
	}
	
	public static int cur_maplet_x () {
		return cur_level.maplet_x ( cur_long );
	}
	
	public static int cur_maplet_y () {
		return cur_level.maplet_y ( cur_lat );
	}
	
	// Because we don't like negative maplet numbers
	// we flip longitude sign.
	// This gives world maplet x increaing to the left.
	//
	// Also note that for the state level,
	// global maplet numbers are useless because
	// the map edge lies at a value that is not an
	// even multiple of the maplet grid.
	
	public int maplet_x ( double _long ) {
		if ( onefile != null )
			_long -= east;
		return (int) (- _long / maplet_dlong);
	}
	
	public int maplet_y ( double _lat ) {
		if ( onefile != null )
			_lat -= south;
		return (int) (_lat / maplet_dlat);
	}
	
	public static double cur_fx () {
		int mx = cur_level.maplet_x ( cur_long );
		double xlong = cur_long;
		
		if ( cur_level.onefile != null )
			xlong -= cur_level.east;
		
		return (-xlong - mx * cur_level.maplet_dlong) / cur_level.maplet_dlong;
	}
	
	public static double cur_fy () {
		int my = cur_level.maplet_y ( cur_lat );
		double xlat = cur_lat;
		
		if ( cur_level.onefile != null )
			xlat -= cur_level.south;
		return (xlat - my * cur_level.maplet_dlat) / cur_level.maplet_dlat;
	}
	
	// Needed to calculate scaling for moves.
	public static double maplet_dlong () {
		return cur_level.maplet_dlong;
	}
	
	public static double maplet_dlat () {
		return cur_level.maplet_dlat;
	}
	
	final String[] lat_code = {"a", "b", "c", "d", "e", "f", "g", "h" };
	
	// The following scheme is used to identify maps within a 1x1 degree
	// area.  First consider the 24K series, where one of 64 different
	// files need to be identified.  We use a designation like b3,
	// where the first letter "counts" a-h from south to north.
	// the second number counts 1-8 from east to west.
	// So, a1 is the bottom right map, h8 is the top left.
	// The 100K and 500K series are more interesting,
	// particularly the 100K.
	// The code "a1" now designates which of the 7.5 minute
	// maps would be in the lower right of the map in question.
	// The 500k series has 1 file per 1x1 degree section
	//  and it always has the "a1" designation.
	//  (the files have a 2x2 maplet layout)
	// The 100k series has 2 files per 1x1 degree section
	//  they have either an a1 or e1 designation.
	//  the a1 file is the bottom half of the 1x1
	//  the e1 file is the top half.
	//  (the files have a 16x8 maplet layout)
	// The 24k series has a 5x10 maplet layout.
	
	// Figure out which map file the coordinates are in.
	// This is the workhorse routine used when we are
	// trying to find a maplet from maplet x/y values.
	//
	// Coordinates and world maplet values from lower left.
	// We see a to h from south to north, as latitude increases
	// We see 1 to 8 from east to west, as neg longitude increases
	// form is "n36112a1.tpq"
	private String encode_map_i ( int world_x, int world_y ) {
		
		//MyView.Log2 ( "encode: wxy", world_x, world_y );
		// number of maplets per degree
		int nmd_x = num_maps_long * num_long;
		int nmd_y = num_maps_lat * num_lat;
		//MyView.Log2 ( "nmd", nmd_x, nmd_y );
		
		if ( onefile != null )
			return onefile;
		
		// MyView.Log ( "wxy " + world_x + " " + world_y );
		
		// This gives integer degrees
		int ilat = world_y / nmd_y;
		int ilong = world_x / nmd_x;
		//MyView.Log ( "encode: ilat/long " + ilat + " " + ilong );
		
		int ix = world_x / num_long - ilong * num_maps_long;
		int iy = world_y / num_lat - ilat * num_maps_lat;
		//MyView.Log2 ( "encode: ixy", ix, iy );
		
		// only needed for 100K
		ix *= quad_long_count;
		iy *= quad_lat_count;
		//MyView.Log2 ( "encode: quad ixy", ix, iy );
		
		// note -- must ensure longitude gets output with 3 digits
		return prefix + ilat + String.format("%03d",ilong) + lat_code[iy] + (ix+1);
	}


        public static boolean maybe () {
                        return true;
        }

}

// THE END
