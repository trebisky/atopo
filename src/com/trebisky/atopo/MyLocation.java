package com.trebisky.atopo;

public class MyLocation {

	private Level level;
	
	// current position, degrees
	private double cur_long;
	private double cur_lat;

	public MyLocation ( Level _lev ) {
		level = _lev;
	}
	
	// Before we allow the user to scroll to a new center location
	// we want to verify that a map is there (this means it will
	// never be possible to move a map edge or corner beyond center).
	private boolean check_map ( double _long, double _lat ) {
		
		tpqFile center_tpq;
		
		// Figure out which map file the coordinates are in.
		// form is something like "n36112a1"
		String map = level.encode_map ( _long, _lat );
		
		// Have to read map header to get information
		center_tpq = level.fetch_map(map);
		if ( ! center_tpq.isvalid() ) {
			return false;
		}
		
		if ( _long < center_tpq.west() || _long > center_tpq.east() || _lat < center_tpq.south() || _lat > center_tpq.north() ) {
			// MyView.setmsg ( "Out of Bounds: " + map );
			// MyView.setmsg ( " W/E: " + center_tpq.west() + " " + center_tpq.east() );
			// MyView.setmsg ( " S/N: " + center_tpq.south() + " " + center_tpq.north() );
			return false;
		}
		return true;
	}
	
	private void new_position ( double _long, double _lat ) {
		
		// set our location for general reference
		cur_long = _long;
		cur_lat = _lat;
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
	
	public double cur_long () {
		return cur_long;
	}
	
	public double cur_lat () {
		return cur_lat;
	}
}

// THE END
