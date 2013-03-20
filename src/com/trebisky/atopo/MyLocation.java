package com.trebisky.atopo;

public class MyLocation {

	// current position, degrees
	private static double cur_long;
	private static double cur_lat;
	
	// public MyLocation ( double _long, double _lat ) {
	// 	cur_long = _long;
	// 	cur_lat = _lat;
	// }

	public static void new_position ( double _long, double _lat ) {
		cur_long = _long;
		cur_lat = _lat;
	}
	
	public static void set(double _long, double _lat) {
		
		// Before we allow the user to move to a new center location
		// we want to verify that a map is there (this means it will
		// never be possible to move a map edge or corner beyond center).
		if ( ! Level.check_map ( _long, _lat ) ) {
			MyView.onemsg("No Map at x,y");
			return;
		}
		
		new_position ( _long, _lat );
	}
	
	public static void jog ( double dlong, double dlat ) {
		double new_long = cur_long + dlong;
		double new_lat = cur_lat + dlat;
		
		if ( Level.check_map ( new_long, new_lat ) ) {
			new_position ( new_long, new_lat );
		}
	}
	
	public static double cur_long () {
		return cur_long;
	}
	
	public static double cur_lat () {
		return cur_lat;
	}
}

// THE END