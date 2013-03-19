package com.trebisky.atopo;

import java.io.File;

// Class to manage level specific data
public class Level {

	private static final int NUM_LEVELS = 5;
	
	// XX should probably change to a Java enum someday
	private static final int L_STATE = 0;
	private static final int L_ATLAS = 1;
	private static final int L_500K = 2;
	private static final int L_100K = 3;
	private static final int L_24K = 4;
	
	private static Level_info levels[] = new Level_info[NUM_LEVELS];
	
	private static Level_info cur_level;
	
	private FileCache file_cache;
	
	private class Level_info {
		public String path;
		public String prefix;
		public int level; 
		
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
			
			for ( File m: list ) {
				String a_file = m.getName();
				
				if ( a_file.length() < 3 )
					continue;
				
				if ( a_file.substring(a_file.length()-3).equals("tpq") ) {
					// return path + "/" + a_file;
					return a_file.substring(0,a_file.length()-4);
				}
			}
			
			return null;
		}
		
		// read some map file to get header info.
		private void probe_map () {
			String probe_map;
			tpqFile tpq;
			
			probe_map = map_to_probe();
			//MyView.Log( "Probing map: " + probe_map);
			
			if ( probe_map == null ) {
				MyView.Log ( "Probe fails");
				MyView.onemsg("Probe_fails");
				return;
			}
			
			// MyView.onemsg("Probing " + probe_map);
			// if ( true ) return;
			
			// tpq = new tpqFile ( probe_path );
			tpq = file_cache.fetch(probe_map);
			
			if ( ! tpq.isvalid() ) {
				MyView.Log ( "Probe fails, bad TPQ: " + probe_map);
				MyView.onemsg ( "Probe fails, bad TPQ: " + probe_map);
				return;
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
		}
		
		public Level_info ( int l, String p, String extra ) {
			level = l;
			path = p;
			
			if ( l == L_STATE || l == L_ATLAS ) {
				onefile = extra;
			} else {
				prefix = extra;
			}
			
			maplet_cache = new MapletCache ();
			
			// keep file_cache from blowing up.
			cur_level = this;
			
			probe_map();
		}
	}
	// End of Level_info class.
	
	// Used in Location class to probe new centers.
	// never fetches Maplets, only tpqFile object
	public tpqFile fetch_map ( String arg ) {
		return file_cache.fetch(arg);
	}
	
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
		rv = cur_level.maplet_cache.fetch( world_x, world_y);
		if ( rv != null )
			return rv;
		
		// special case for state and atlas levels
		if ( cur_level.onefile != null ) {
			//MyView.Log ( "maplet_lookup: " + cur_level.onefile );
			tpq = fetch_map ( cur_level.onefile );
			//MyView.Log ( "maplet_lookup: tpq " + tpq );
			sheet_x = world_x;
			sheet_y = world_y;
			//MyView.Log2( "maplet_lookup: sheet_xy" , sheet_x, sheet_y);
		} else {
			name = encode_map_i ( world_x, world_y );
			//MyView.Log ( "maplet_lookup: " + name );
			tpq = fetch_map ( name );
			//MyView.Log ( "maplet_lookup: tpq " + tpq );
			
			map_x = world_x / cur_level.num_long;
			map_y = world_y / cur_level.num_lat;
			//MyView.Log( "maplet_lookup: mx,my " + map_x + " " + map_y );
		
			sheet_x = world_x - map_x * cur_level.num_long;
			sheet_y = world_y - map_y * cur_level.num_lat;
			//MyView.Log2( "maplet_lookup: sheet_xy " , sheet_x, sheet_y);
		}
		
		//MyView.Log2( "maplet_lookup: num_xy" , cur_level.num_long, cur_level.num_lat );
		// world_x comes in counting right to left.
		// world_y comes in counting bottom to top.
		// sheet_x needs to count from left to right.
		// sheet_y needs to count from top to botom.
		sheet_x = cur_level.num_long - sheet_x - 1;
		sheet_y = cur_level.num_lat - sheet_y - 1;
		//MyView.Log2( "maplet_lookup: sheet_xy " , sheet_x, sheet_y);
		
		if ( sheet_x < 0 || sheet_x >= cur_level.num_long ) { return null; }
		if ( sheet_y < 0 || sheet_y >= cur_level.num_lat ) { return null; }
		
		idx = sheet_y * cur_level.num_long + sheet_x;
		//MyView.Log( "maplet_lookup: idx " + idx);
		
		rv = cur_level.maplet_cache.load( world_x, world_y, tpq, idx );
		return rv;
	}
	
	public Level ( String base ) {
		
		file_cache = new FileCache ();
		
		levels[0] = new Level_info ( L_STATE, base, "us1map1" );
		levels[1] = new Level_info ( L_ATLAS, base, "us1map2" );
		levels[2] = new Level_info ( L_500K, base + "/l3", "g" );
		levels[3] = new Level_info ( L_100K, base + "/l4", "c" );
		levels[4] = new Level_info ( L_24K, base + "/l5", "n" );
		
		set_level ( L_24K );
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
		if ( cur_level.onefile != null )
			_long -= cur_level.east;
		return (int) (- _long / cur_level.maplet_dlong);
	}
	
	public int maplet_y ( double _lat ) {
		if ( cur_level.onefile != null )
			_lat -= cur_level.south;
		return (int) (_lat / cur_level.maplet_dlat);
	}
	
	public double fx ( double _long ) {
		int mx = maplet_x ( _long );
		if ( cur_level.onefile != null )
			_long -= cur_level.east;
		return (-_long - mx * cur_level.maplet_dlong) / cur_level.maplet_dlong;
	}
	
	public double fy ( double _lat ) {
		int my = maplet_y ( _lat );
		if ( cur_level.onefile != null )
			_lat -= cur_level.south;
		return (_lat - my * cur_level.maplet_dlat) / cur_level.maplet_dlat;
	}
	
	public int num_long () {
		return cur_level.num_long;
	}
	
	public int num_lat () {
		return cur_level.num_lat;
	}
	
	public double maplet_dlong () {
		return cur_level.maplet_dlong;
	}
	
	public double maplet_dlat () {
		return cur_level.maplet_dlat;
	}
	
	private void set_level ( int arg ) {
		cur_level = levels[arg];
	}
	
	public void set_state () {
		set_level ( L_STATE );
	}
	
	public void set_atlas () {
		set_level ( L_ATLAS );
	}
	
	public void set_500k () {
		set_level ( L_500K );
	}
	
	public void set_100k () {
		set_level ( L_100K );
	}
	
	public void set_24k () {
		set_level ( L_24K );
	}
	
	public static void up () {
		if ( cur_level.level <= 0 )
			return;
		
		int newl = cur_level.level - 1;
		cur_level = levels[newl];
	}
	
	public static void down () {
		if ( cur_level.level >= NUM_LEVELS - 1 )
			return;
		
		int newl = cur_level.level + 1;
		cur_level = levels[newl];
	}

	// Figure out which map file the coordinates are in.
	// form is "n36112a1.tpq"
	// Called when we only want to get into from the
	// map header.  Used in Location.check_map to verify
	// that maps exist under a given long, lat and in
	// MyView.onDraw to get header info for the center
	// maplet.
	public String encode_map ( double _long, double _lat ) {
		
		return encode_map_i ( maplet_x(_long), maplet_y(_lat) );
		
		//if ( cur_level.onefile != null )
		//	return cur_level.onefile;
			
		//int ilat = (int) _lat;
		//int ilong = (int) _long;
		//Log.w ( "aTopo", "find map: " + ilong + " " + ilat );
		
		//int ix = (int) (-(_long-ilong) / cur_level.map_long);
		//int iy = (int) ((_lat-ilat) / cur_level.map_lat);
		//Log.w ( "aTopo", "find map(i): " + ix + " " + iy );
		
		//ilong = -ilong;
		//return cur_level.prefix + ilat + ilong + lat_code[iy] + (ix+1);
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
		int nmd_x = cur_level.num_maps_long * cur_level.num_long;
		int nmd_y = cur_level.num_maps_lat * cur_level.num_lat;
		//MyView.Log2 ( "nmd", nmd_x, nmd_y );
		
		if ( cur_level.onefile != null )
			return cur_level.onefile;
		
		// MyView.Log ( "wxy " + world_x + " " + world_y );
		
		// This gives integer degrees
		int ilat = world_y / nmd_y;
		int ilong = world_x / nmd_x;
		//MyView.Log ( "encode: ilat/long " + ilat + " " + ilong );
		
		int ix = world_x / cur_level.num_long - ilong * cur_level.num_maps_long;
		int iy = world_y / cur_level.num_lat - ilat * cur_level.num_maps_lat;
		//MyView.Log2 ( "encode: ixy", ix, iy );
		
		// only needed for 100K
		ix *= cur_level.quad_long_count;
		iy *= cur_level.quad_lat_count;
		//MyView.Log2 ( "encode: quad ixy", ix, iy );
		
		// This gives maplet counts in the degree.
		// divide to give map in the degree.
		// int ix = (world_x - ilong * nmd_x);
		// int iy = (world_y - ilat * nmd_y);
		// MyView.Log ( "x/y A" + ix + " " + iy );
		
		// ix = ix / cur_level.num_long;
		// iy = iy / cur_level.num_lat;
		// MyView.Log ( "x/y B" + ix + " " + iy );
		//Log.w ( "aTopo", "find map(i): " + ix + " " + iy );
		
		return cur_level.prefix + ilat + ilong + lat_code[iy] + (ix+1);
	}
	
	public static String base_path () {
		if ( cur_level == null || cur_level.path == null ) {
			MyView.Log ( "cur_level base_path is broken");
			return null;
		}
		return cur_level.path;
	}

}

// THE END