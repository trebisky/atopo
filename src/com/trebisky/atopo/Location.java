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
	
	public tpqFile bogus_tpq () {
		return tpq;
	}
	
	public void set(double _long, double _lat) {
		double dlong, dlat;		// from map edge
		int tile_upy;
		
		// Figure out which map file the coordinates are in.
		// form is "n36112a1.tpq"
		String map = level.find_map ( _long, _lat );
		tpq = new tpqFile ( map );
		
		if ( _long < tpq.west() || _long > tpq.east() || _lat < tpq.south() || _lat > tpq.north() ) {
			MyView.setmsg ( "Out of Bounds");
			MyView.nomaps ();
			return;
		}
		
		// bogus_setup();
		
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
		
		// tile_long = (tile_x - 1) * maplet_dlong;
		// tile_lat = (tile_upy - 1) * maplet_dlat;
		
		fx = (dlong - tile_x*m_dlong) / m_dlong;
		fy = (dlat - tile_upy*m_dlat) / m_dlat;
	}
	
	public void jog ( double _long, double _lat ) {
		set ( cur_long + _long, cur_lat + _lat );
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