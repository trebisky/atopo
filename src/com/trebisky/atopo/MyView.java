package com.trebisky.atopo;

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

	private static final int NUM_MSG = 10;
	private static boolean no_maps = false;
	private static String[] msg = new String[NUM_MSG];
	private static int msg_index;
	
	private Level level;
	private Location location;
	
	private static final String TAG = "atopo";
	
	public static void Log ( String msg ) {
		Log.e ( TAG, msg );
	}

	private Paint myPaint;

	// degrees per pixel
	private double scalex, scaley;

	// motion stuff ...
	private boolean have_last = false;
	private int lastx, lasty;
	
	// cache
	private static HashMap maplet_cache;
	private static FileCache file_cache;
	
	// XXX wart
	public static FileCache file_cache () {
		return file_cache;
	}
	
	private tpqTile getTile ( int _x, int _y ) {
		int key;
		tpqTile rv;
		
		if ( _x < 0 || _x >= level.num_long() ) { return null; }
		if ( _y < 0 || _y >= level.num_lat() ) { return null; }
		
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
			if ( x < 0 || x >= level.num_long() ) {
				map = null;
				return;
			}
			if ( y < 0 || y >= level.num_lat() ) {
				map = null;
				return;
			}
			x = _x;
			y = _y;
			
			int idx = _y * level.num_long() + _x;
			loadTile(idx);
			if ( map == null ) {
				Log.e ( TAG, "bad xy = " + x + "  " + y);
			}
		}
		
		// hack for special use
		public tpqTile(int idx) {
			loadTile(idx);
		}

		private void loadTile(int idx) {
			
			// XXX
			tpqFile My_tpq = location.bogus_tpq();
			
			int offset = My_tpq.offset(idx);
			if ( offset <= 0 ) {
				Log.e(TAG,"loadTile - bad tpq index: " + idx);
				map = null;
				return;
			}
			
			int length = My_tpq.size(idx);
			RandomAccessFile rfile = My_tpq.rfile();
			if ( rfile == null ) {
				map = null;
				return;
			}
			
			byte[] jpeg_data = new byte[length];

			try {
				rfile.seek(offset);
				rfile.read(jpeg_data, 0, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				map = null;
				return;
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
		maplet_cache = new HashMap ();
		file_cache = new FileCache ();
	}
	
	// I call this after instantiation,
	// could be part of constructor.
	public void setup ( Level _level, Location _loc ) {
		level = _level;
		location = _loc;
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
	
	// must call this if you want to use setmsg()
	// and see the output.
	public static void nomaps () {
		no_maps = true;
	}
	
	public static void setmsg ( String arg ) {
		if ( msg_index >= NUM_MSG ) return;
		msg[msg_index++] = arg;
	}
	
	public static void onemsg ( String arg ) {
		setmsg ( arg );
		nomaps ();
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
		center_maplet = getTile ( location.maplet_x(), location.maplet_y() );

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
		
		scalex = level.maplet_dlong() / px;
		scaley = level.maplet_dlat() / py;
		
		offx = (int) (location.fx() * px);
		offy = (int) (location.fy() * py);
		
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

	public void handle_move ( int dx, int dy ) {
		double delta_long;
		double delta_lat;
		
		delta_long = dx * scalex;
		delta_lat = dy * scaley;
		
		location.jog ( -delta_long,  delta_lat );
		
		this.invalidate();
	}
	
	@Override
	public boolean onTouchEvent ( MotionEvent e ) {
		
		int action = e.getAction();
		int x, y;
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			//Log.w(TAG,"Touch - down");
			have_last = false;	// really !
			return true;
		} else if ( action == MotionEvent.ACTION_UP ) {
			//Log.w(TAG,"Touch - up");
			have_last = false;
			return true;
		} else if ( action == MotionEvent.ACTION_MOVE ) {
			x = (int) e.getX();
			y = (int) e.getY();
			//Log.w(TAG,"Touch - move: " + x + " " + y);
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