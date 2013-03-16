package com.trebisky.atopo;

import java.io.File;

import android.util.Log;

// Class to manage level specific data
public class Level {

	// XX should probably change to a Java enum someday
	private static final int L_STATE = 0;
	private static final int L_ATLAS = 1;
	private static final int L_500K = 2;
	private static final int L_100K = 3;
	private static final int L_24K = 4;
	
	private static final int NUM_LEVELS = 5;
	
	// private int levels[] = { L_STATE, L_ATLAS, L_500K, L_100K, L_24K };
	
	private class Level_info {
		public String path;
		public int level; 
		public boolean file = false;
		
		// overall size of maps in degrees
		public double map_long, map_lat;
		
		// number of maplets in a map file
		public int num_long, num_lat;
		
		// maplet size in degrees
		public double maplet_dlong, maplet_dlat;
		
		// maps per degree (for level).
		public int num_maps_long, num_maps_lat;
		
		// read some map file to get header info.
		private void probe_map () {
			File f = new File ( path );
			
			// It bothers me to do this, given that there may
			// be 12,000 entries in the list and I only want the
			// first one, but I don't know how better to do this.
			// It goes on the stack and should evaporate,
			// and it happens only once at startup.
			// it does work just fine.
			File [] list = f.listFiles();	// may be huge !
			tpqFile tpq = null;
			
			for ( File m: list ) {
				String one_file = m.getName();
				
				if ( one_file.length() < 3 )
					continue;
				
				if ( one_file.substring(one_file.length()-3).equals("tpq") ) {
					tpq = new tpqFile ( path + "/" + one_file );
					break;
				}
			}
			if ( tpq == null ) {
				Log.e ( "aTopo", "Probe fails");
			}
				
			if ( tpq != null && tpq.isvalid() ) {
				map_long = tpq.east() - tpq.west();
				map_lat = tpq.north() - tpq.south();
				
				num_long = tpq.num_long();
				num_lat = tpq.num_lat();
				
				maplet_dlong = map_long / num_long;
				maplet_dlat = map_lat / num_lat;
				
				num_maps_long = (int) ( 0.9999 / map_long);
				num_maps_lat = (int) (0.9999 / map_lat);
			}
		}
		
		public Level_info ( int l, String p ) {
			level = l;
			path = p;
			
			if ( l == L_STATE || l == L_ATLAS ) {
				file = true;
				return;
			}
			
			probe_map();
		}
	}
	
	private Level_info levels[] = new Level_info[NUM_LEVELS];
	
	private Level_info cur_level;
	
	public Level ( String base ) {
		
		levels[0] = new Level_info ( L_STATE, base + "/us1map1.tpq" );
		levels[1] = new Level_info ( L_ATLAS, base + "/us1map2.tpq");
		levels[2] = new Level_info ( L_500K, base + "/l3" );
		levels[3] = new Level_info ( L_100K, base + "/l4" );
		levels[4] = new Level_info ( L_24K, base + "/l5" );
		
		set_level ( L_24K );
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
	
	public void up () {
		if ( cur_level.level <= 0 )
			return;
		set_level ( cur_level.level - 1 );
	}
	
	public void down () {
		if ( cur_level.level >= NUM_LEVELS - 1 )
			return;
		set_level ( cur_level.level + 1 );
	}

	final String[] lat_code = {"a", "b", "c", "d", "e", "f", "g", "h" };
	
	// Figure out which map file the coordinates are in.
	// form is "n36112a1.tpq"
	public String find_map ( double _long, double _lat ) {
		
		if ( cur_level.file )
			return cur_level.path;
			
		int ilat = (int) _lat;
		int ilong = (int) _long;
		Log.w ( "aTopo", "find map: " + ilong + " " + ilat );
		
		int ix = (int) (-(_long-ilong) / cur_level.map_long);
		int iy = (int) ((_lat-ilat) / cur_level.map_lat);
		Log.w ( "aTopo", "find map(i): " + ix + " " + iy );
		
		ilong = -ilong;
		return cur_level.path + "/n" + ilat + ilong + lat_code[iy] + (ix+1) + ".tpq";
	}

}

// THE END