package com.trebisky.atopo;

public class Location {

	private Level level;
	
	// current position, degrees
	private double cur_long;
	private double cur_lat;

	// current position, within center file
	private double fx;
	private double fy;

	// current position, world maplet index
	private int tile_x;
	private int tile_y;

	public Location ( Level _lev ) {
		level = _lev;
	}
	
	private tpqFile tpq;
	
	// XXX
	public tpqFile bogus_tpq () {
		return tpq;
	}
	
	// Before we allow the user to scroll to a new center location
	// we want to verify that a map is there (this means it will
	// never be possible to move a map edge or corner beyond center).
	private boolean check_map ( double _long, double _lat ) {
		
		// Figure out which map file the coordinates are in.
		// form is something like "n36112a1"
		String map = level.find_map ( _long, _lat );
		
		// Have to read map header to get information
		// tpq = new tpqFile ( map );
		tpq = MyView.file_cache().get(map);
		if ( ! tpq.isvalid() ) {
			return false;
		}
		
		if ( _long < tpq.west() || _long > tpq.east() || _lat < tpq.south() || _lat > tpq.north() ) {
			// MyView.setmsg ( "Out of Bounds: " + map );
			// MyView.setmsg ( " W/E: " + tpq.west() + " " + tpq.east() );
			// MyView.setmsg ( " S/N: " + tpq.south() + " " + tpq.north() );
			// MyView.nomaps ();
			return false;
		}
		return true;
	}
	
	private void new_position ( double _long, double _lat ) {
		double dlong, dlat;		// from map edge
		int tile_upy;
		
		// set our location for general reference
		cur_long = _long;
		cur_lat = _lat;
		
		// from map edge (lower left)
		dlong = cur_long - tpq.west();
		dlat = cur_lat - tpq.south();
		
		double m_dlong = level.maplet_dlong();
		double m_dlat = level.maplet_dlat();
		
		tile_x = (int) (dlong / m_dlong );
		tile_upy = (int) (dlat / m_dlat );
		tile_y = level.num_lat() - tile_upy - 1;
		//MyView.setmsg("tile_xy " + tile_x + " " + tile_y );
		//MyView.nomaps();
		
		// tile_long = (tile_x - 1) * maplet_dlong;
		// tile_lat = (tile_upy - 1) * maplet_dlat;
		
		fx = (dlong - tile_x*m_dlong) / m_dlong;
		fy = (dlat - tile_upy*m_dlat) / m_dlat;
	}
	
	public void set(double _long, double _lat) {
		
		if ( ! check_map ( _long, _lat ) ) {
			MyView.onemsg("No Map at x,y");
			return;
		}
		
		new_position ( _long, _lat );
		
	}
	
	public void jog ( double dlong, double dlat ) {
		double new_long = cur_long + dlong;
		double new_lat = cur_lat + dlat;
		
		if ( check_map ( new_long, new_lat ) ) {
			new_position ( new_long, new_lat );
		}
	}
	
	public int maplet_x () {
		return tile_x;
	}
	
	public int maplet_y () {
		return tile_y;
	}
	
	public double fx () {
		return fx;
	}
	
	public double fy () {
		return fy;
	}
}

// THE END