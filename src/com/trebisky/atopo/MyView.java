package com.trebisky.tpqreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MyView extends View {

	private static final boolean drop_dead = true;
	
	private static final int NUM_MSG = 10;
	private boolean no_maps = false;
	private String[] msg = new String[NUM_MSG];
	private int msg_index;
	
	private static final String TAG = "TOPO";

	private Paint myPaint;

	private String tpq_dir;
	private RandomAccessFile rfile;
	private long file_size;
	
	private int[] index;
	private int[] size;
	private int n_index;

	private boolean run_seq = false;
	private int next_map;

	private double my_long;
	private double my_lat;
	
	private double my_fx;
	private double my_fy;

	private int num_long;
	private int num_lat;
	
	/* size of a map in degrees */
	private double map_long, map_lat;
	
	/* number of maps in a degree */
	private int num_maps_long, num_maps_lat;
	
	private double elong, wlong;
	private double slat, nlat;

	private double maplet_dlong;
	private double maplet_dlat;
	
	private double scalex, scaley;

	private int tile_x;
	private int tile_y;

	// private tpqTile draw_tile;
	
	// motion stuff ...
	private boolean have_last = false;
	private int firstx, firsty;
	private int lastx, lasty;
	
	// cache
	HashMap maplet_cache;
	
	private tpqTile getTile ( int _x, int _y ) {
		int key;
		tpqTile rv;
		
		if ( _x < 0 || _x >= num_long ) { return null; }
		if ( _y < 0 || _y >= num_lat ) { return null; }
		
		key = _y * 1000 + _x;
		rv = (tpqTile) maplet_cache.get(key);
		
		if ( rv == null ) {
			rv = new tpqTile ( _x, _y );
			if ( rv.map != null )
				maplet_cache.put(key, rv);
			else
				rv = null;
		}
		return rv;
	}

	class tpqTile {
		int x; /* indices in the TPQ file */
		int y;
		Bitmap map;
		
		public tpqTile(int _x, int _y) {
			//Log.w(TAG,"tpq Tile: " + _x + "  " + _y);
			if ( x < 0 || x >= num_long ) {
				map = null;
				return;
			}
			if ( y < 0 || y >= num_lat ) {
				map = null;
				return;
			}
			x = _x;
			y = _y;
			
			int idx = _y * num_long + _x;
			//Log.w(TAG,"tpq Tile(long) " + num_long );
			loadTile(idx);
			if ( map == null ) {
				Log.e ( TAG, "bad xy = " + x + "  " + y);
			}
		}
		
		// hack for special use
		public tpqTile(int idx) {
			loadTile(idx);
		}

		// This is a hack to support display
		// of a plain old jpeg file.
		public tpqTile ( String path ) {
			File imgFile = new File(path);

			if (imgFile.exists()) {
				map = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			}
		}

		private void loadTile(int idx) {
			if ( idx < 0 || idx >= n_index ) {
				Log.e(TAG,"bad tpq index: " + idx);
				map = null;
				return;
			}
				
			int offset = index[idx];
			int length = size[idx];
			
			byte[] jpeg_data = new byte[length];

			try {
				rfile.seek(offset);
				rfile.read(jpeg_data, 0, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Don't know why, but the following always returned null
			// myBitmap = BitmapFactory.decodeFileDescriptor ( rfile.getFD() );

			map = BitmapFactory.decodeByteArray(jpeg_data, 0, length);
		}
		
		public int getWidth () {
			return map.getWidth();
		}
		
		public int getHeight () {
			return map.getHeight();
		}
	}

	private void init() {
		myPaint = new Paint();
		myPaint.setColor(Color.BLACK);
		myPaint.setTextSize(20);
		next_map = 0;
		maplet_cache = new HashMap ();
	}

	public MyView(Context context) {
		super(context);
		init();
		// TODO Auto-generated constructor stub
	}

	public MyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		// TODO Auto-generated constructor stub
	}

	public MyView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		init();
		// TODO Auto-generated constructor stub
	}
	
	public void nomaps () {
		no_maps = true;
	}
	
	public void setmsg ( String arg ) {
		if ( msg_index >= NUM_MSG ) return;
		msg[msg_index++] = arg;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int ox, oy;
		int ex, ey;
		int cw, ch; // size of canvas
		int cx, cy; // center of canvas
		String bad = "NULL";
		
		int px, py;
		int offx, offy;
		int nx1, nx2;
		int ny1, ny2;
		tpqTile center_maplet;

		canvas.drawColor(Color.BLUE);
		
		// A great way to do some debugging
		for ( int i=0; i< msg_index; i++ ) {
			canvas.drawText(msg[i], 100, 100 + i * 30, myPaint);
		}
		
		if ( no_maps ) return;

		cw = canvas.getWidth();
		ch = canvas.getHeight();
		
		cx = cw / 2;
		cy = ch / 2;
		
		// We really only need to get this here
		// in this routine so that we have access
		// to values for px, py, someday we will
		// get those at TPQ initialization time
		// (maybe from the header), in which case
		// we will get rid of this here AND let
		// the loop below handle the 0,0 case
		center_maplet = getTile ( tile_x, tile_y );

		//Log.w(TAG, "in onDraw " + draw_tile);
		if (center_maplet == null) {
			int tx = cx - (int) myPaint.measureText(bad);
			canvas.drawText(bad, tx, cy, myPaint);
			return;
		}
		
		// XXX
		// eventually this will be a global thing.
		// it is constant for a given TPQ file.
		px = center_maplet.getWidth();
		py = center_maplet.getHeight();
		
		scalex = maplet_dlong / px;
		scaley = maplet_dlat / py;
		
		offx = (int) (my_fx * px);
		offy = (int) (my_fy * py);
		
		// This puts the chosen location in the
		// center tile in the center of the canvas.
		ox = cx - offx;
		oy = cy - (py - offy);
		
		// This centers the center tile.
		//ox = (cw - px )/2;
		//oy = (ch - py )/2;
		
		canvas.drawBitmap(center_maplet.map, ox, oy, null);
		
		// counts from left to right
		nx1 = - ( ox + (px - 1)) / px;
		nx2 = + ( cw - (ox + px) + (px - 1)) / px;
		
		// counts from top to bottom
		ny1 = - ( oy + (py - 1)) / py;
		ny2 = + ( ch - (oy + py) + (py - 1)) / py;
		
		//msg1 = "cwh = " + cw + " " + ch;
		//msg1 = "fx = " + my_fx + " " + my_fy;
		//msg2 = "px = " + px + " " + py;
		//msg3 = "offx = " + offx + " " + offy;
		//msg4 = "ox = " + ox + " " + oy;
		
		//msg2 = "nx1,nx2 = " + nx1 + " " + nx2;
		//msg3 = "ny1,ny2 = " + ny1 + " " + ny2;
		
		/*
		ox = ox + draw_tile.getWidth();
		oy = ch - draw_tile.getHeight();
		canvas.drawBitmap(draw_tile.map, ox, oy, null);
		*/
		
		/*
		int xx = 1;
		int yy = 1;
		tpqTile extra = new tpqTile ( draw_tile.x +xx, draw_tile.y +yy );
		ex = ox + xx * extra.getWidth();
		ey = oy + yy * extra.getHeight();
		canvas.drawBitmap(extra.map, ex, ey, null);
		*/
		
		//if ( drop_dead )
		//	return;
		
		for ( int xx = nx1; xx <= nx2; xx++ ) {
			for ( int yy = ny1; yy <= ny2; yy++ ) {
				if ( xx == 0 && yy == 0 ) {
					continue;
				}
				tpqTile extra = getTile ( center_maplet.x +xx, center_maplet.y +yy );
				ex = ox + xx * px;
				ey = oy + yy * py;
				if ( extra == null ) {
					// canvas.drawText("MAP", ex, ey, myPaint);
				    continue;
				}
				canvas.drawBitmap(extra.map, ex, ey, null);
			}
		}

		// A great way to do some debugging
		// canvas.drawText(msg1, 100, 100, myPaint);
		// canvas.drawText(msg2, 100, 150, myPaint);
		// canvas.drawText(msg3, 100, 200, myPaint);
		// canvas.drawText(msg4, 100, 250, myPaint);
	}

	// display a single .jpg or .png file
	public void setImage(String impath) {
		// draw_tile = new tpqTile ( impath );
	}

	// called whenver timer ticks and we should
	// go to the next file.
	public void nextFile() {

		if (!run_seq) {
			return;
		}

		++next_map;
		if (next_map >= n_index) {
			next_map = 0;
		}
		//msg3 = "Showing: " + next_map;

		// draw_tile = new tpqTile(next_map);
	}

	// read 2 bytes
	// 0x8950 is a PNG ?
	// 0xFFD8 is a JPG (swapped)
	private boolean is_jpeg(RandomAccessFile rf, int offset) {
		int val;

		try {
			rf.seek(offset);
			val = rf.readShort() & 0xffff;
		} catch (IOException e) {
			return false;
		}

		// test it against the swapped constant
		return val == 0xFFD8 ? true : false;
	}

	// This reads the entire index, but then scans to count how
	// many pointers are actually JPEG data (these always come first),
	// then returns a reduced count.
	private void load_index() throws IOException {
		
		int off1;
		int [] x_index;
		int count;
		int njpeg;
		int i;
		
		rfile.seek(1024);
		off1 = Integer.reverseBytes(rfile.readInt());
		rfile.seek(1024);
		
		count = (off1 - 1024) / 4 - 4;
		x_index = new int[count];

		// int nbytes = num * 4;
		// byte[] index_buf;
		// index_buf = new byte[nbytes];
		// rfile.read(index_buf, 0, nbytes );
		// fix_index(index_buf, index, n_index);

		// msg1 = "Total tiles: " + count;

		for (i = 0; i < count; i++) {
			x_index[i] = Integer.reverseBytes(rfile.readInt());
		}
		
		// It is tempting to put the is_jpeg testing below
		// into the loop above, but it would involve
		// seeking back and forth each time, so we don't

		// Note that for us1map2.tpq there are 6133 indices
		// of which only 1534 point to tiles
		// (this map is 59 by 26)
		for ( njpeg = 0; njpeg < count; njpeg++) {
			if ( ! is_jpeg(rfile, x_index[njpeg])) {
				break;
			}
		}
		//Log.w ( TAG, "Jpeg tiles: " + njpeg );
		
		size = new int[njpeg];
		
		// some TPQ files have no useless pointers
		// after the JPEG pointers, so we must take
		// care to avoid a out of bounds array reference.
		for ( i=0; i<njpeg; i++ ) {
			if ( i+1 < count )
				size[i] = x_index[i+1] - x_index[i];
			else
				size[i] = (int) file_size - x_index[i];
		}
		
		index = new int[njpeg];
		n_index = njpeg;
		
		System.arraycopy( x_index, 0, index, 0, njpeg );
	}

	private void read_header() throws IOException {
		int junk;

		junk = rfile.readInt(); // version
		
		wlong = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		nlat = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		elong = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		slat = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		
		map_long = elong - wlong;
		map_lat = nlat - slat;
		
		num_maps_long = (int) ( 0.9999 / map_long );
		num_maps_lat = (int) (0.9999 / map_lat );
		
		// setmsg ( "West: " + west );
		// setmsg ( "East: " + east );
		
		rfile.skipBytes(456);
		num_long = Integer.reverseBytes(rfile.readInt());
		num_lat = Integer.reverseBytes(rfile.readInt());

		maplet_dlong = (elong - wlong) / num_long;
		maplet_dlat = (nlat - slat) / num_lat;
	}
	
	public void handle_move ( int dx, int dy ) {
		double delta_long;
		double delta_lat;
		
		delta_long = dx * scalex;
		delta_lat = dy * scaley;
		
		setLoc ( my_long - delta_long, my_lat + delta_lat );
		this.invalidate();
	}
	
	public void setLoc(double lng, double lat) {
		int tile_upy;
		double dlong, dlat;		// from map edge
		// double tile_long, tile_lat;
		tpqTile preload;
		
		if ( lng < wlong || lng > elong || lat < slat || lat > nlat ) {
			setmsg ( "Out of Bounds");
			nomaps ();
			return;
		}
		
		my_long = lng;
		my_lat = lat;
		
		// from map edge (lower left)
		dlong = my_long - wlong;
		dlat = my_lat - slat;
		
		tile_x = (int) (dlong / maplet_dlong);
		tile_upy = (int) (dlat / maplet_dlat);
		tile_y = num_lat - tile_upy - 1;
		
		// tile_long = (tile_x - 1) * maplet_dlong;
		// tile_lat = (tile_upy - 1) * maplet_dlat;
		
		my_fx = (dlong - tile_x*maplet_dlong) / maplet_dlong;
		my_fy = (dlat - tile_upy*maplet_dlat) / maplet_dlat;

		// msg4 = "tile_xy = " + tile_x + "  " + tile_y;
		//Log.w(TAG, "in setLoc " + draw_tile);
		
		// We do this here, so this tile gets into the cache
		// before onDraw is called.
		preload = new tpqTile(tile_x, tile_y);
	}
	
	public void setDir ( String dir ) {
		tpq_dir = dir;
	}
	
	final String[] lat_code = {"a", "b", "c", "d", "e", "f", "g", "h" };
	
	// Figure out which map file the coordinates are in.
	// form is "n36112a1.tpq"
	public void setMap ( double lng, double lat ) {
		
		int ilat = (int) lat;
		int ilong = (int) lng;
		int ix = (int) (-(lng-ilong) / map_long);
		int iy = (int) ((lat-ilat) / map_lat);
		
		ilong = -ilong;
		String path = tpq_dir + "/n" + ilat + ilong + lat_code[iy] + (ix+1) + ".tpq";
		
		// setmsg ( path );
		// nomaps ();
		setTPQ ( path );
	}

	public void setTPQ(String path) {

		File tpqfile;

		try {
			tpqfile = new File(path);
			file_size = tpqfile.length();
			rfile = new RandomAccessFile(tpqfile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			read_header();
			load_index();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.w ( TAG, "num_long = " + num_long );
		Log.w ( TAG, "num_lat = " + num_lat );
		Log.w ( TAG, "n_index = " + n_index );

		// msg4 = "maplets: " + num_long + " by " + num_lat;

		// draw_tile = new tpqTile ( 0 );
		
		Log.w ( TAG, "memory limit: " + Runtime.getRuntime().maxMemory() );
		Log.w ( TAG, "memory limit: " + Runtime.getRuntime().maxMemory() );
	}
	
	@Override
	public boolean onTouchEvent ( MotionEvent e ) {
		
		int action = e.getAction();
		int x, y;
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			Log.w(TAG,"Touch - down");
			have_last = false;	// really !
			return true;
		} else if ( action == MotionEvent.ACTION_UP ) {
			Log.w(TAG,"Touch - up");
			have_last = false;
			return true;
		} else if ( action == MotionEvent.ACTION_MOVE ) {
			x = (int) e.getX();
			y = (int) e.getY();
			Log.w(TAG,"Touch - move: " + x + " " + y);
			if ( ! have_last ) {
				lastx = x;
				lasty = y;
				have_last = true;
				return true;
			}
			handle_move ( x-lastx, y-lasty );
			lastx = x;
			lasty = y;
			return true;
		} else {
			return super.onTouchEvent(e);
		}
	}
}