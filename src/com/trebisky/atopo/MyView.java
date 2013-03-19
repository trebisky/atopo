package com.trebisky.atopo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MyView extends View {

	private static final int NUM_MSG = 10;
	private static String[] msg = new String[NUM_MSG];
	private static int msg_index;
	
	private Level level;
	private MyLocation location;
	
	private static final String TAG = "atopo";
	
	public static void Log ( String msg ) {
		Log.e ( TAG, msg );
	}
	
	public static void Log2 ( String msg, int a, int b ) {
		Log.e ( TAG, msg + " " + a + " " + b );
	}
	
	public static void Log2d ( String msg, double a, double b ) {
		Log.e ( TAG, msg + " " + a + " " + b );
	}

	private Paint myPaint;

	// degrees per pixel
	private double scalex, scaley;

	private void init() {
		myPaint = new Paint();
		myPaint.setColor(Color.BLACK);
		myPaint.setTextSize(20);
	}
	
	// I call this after instantiation,
	// could be part of constructor.
	public void setup ( Level _level, MyLocation _loc ) {
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
	
	public static void setmsg ( String arg ) {
		if ( msg_index >= NUM_MSG ) return;
		msg[msg_index++] = arg;
	}
	
	public static void onemsg ( String arg ) {
		setmsg ( arg );
	}
	
	private void drawBox ( Canvas canvas, int ox, int oy, int px, int py ) {
		
		canvas.drawLine ( ox, oy, ox+px, oy, myPaint );
		canvas.drawLine ( ox, oy, ox, oy+py, myPaint );
		canvas.drawLine ( ox+px, oy, ox+px, oy+py, myPaint );
		canvas.drawLine ( ox, oy+py, ox+px, oy+py, myPaint );
	}
	
	private void marker ( Canvas canvas, int x, int y ) {
		canvas.drawLine ( x-20, y, x+20, y, myPaint );
		canvas.drawLine ( x, y-20, x, y+20, myPaint );
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		double center_long, center_lat;
		int ox, oy;
		int ex, ey;
		int cw, ch; // size of canvas
		int cx, cy; // center of canvas
		double fx, fy;
		
		int px, py;
		int offx, offy;
		int nx1, nx2;
		int ny1, ny2;
		
		Maplet center_maplet;
		
		final boolean show_boxes = true;

		// Sometimes this shows through
		// as narrow vertical blue lines
		// as we scroll into a region with
		// different pixel width for maplets.
		canvas.drawColor(Color.BLUE);
		
		// A great way to do some debugging
		for ( int i=0; i< msg_index; i++ ) {
			canvas.drawText(msg[i], 100, 100 + i * 30, myPaint);
		}
		
		if ( msg_index > 0 ) return;
		
		center_long = location.cur_long();
		center_lat = location.cur_lat();
		
		// Figure out which map file the coordinates are in.
		// form is something like "n36112a1"
		String map = level.encode_map ( center_long, center_lat );
		//Log ( "Draw: " + map + " " + center_long + " " + center_lat );
				
		// fetch/read map header
		tpqFile center_tpq = level.fetch_map(map);
		if ( ! center_tpq.isvalid() ) {
			return;
		}
		
		// world maplet x/y from lower right
		// X increasing to left, Y increasing up.
		int maplet_x = level.maplet_x(center_long);
		int maplet_y = level.maplet_y(center_lat);
		//Log ( "Draw: " + map + " " + maplet_x + " " + maplet_y );
		
		fx = level.fx(center_long);
		fy = level.fy(center_lat);
		
		// canvas size
		cw = canvas.getWidth();
		ch = canvas.getHeight();
		
		// canvas center
		cx = cw / 2;
		cy = ch / 2;
		
		// XXX XXX
		// We really only need to get this here
		// in this routine so that we have access
		// to values for px, py, someday we will
		// get those at TPQ initialization time
		// (maybe from the header), in which case
		// we will get rid of this here AND let
		// the loop below handle the 0,0 case
		
		// center_maplet = level.maplet_lookup ( sheet_x, sheet_y );
		center_maplet = level.maplet_lookup ( maplet_x, maplet_y );
		// Log ( "View mx,my A " +maplet_x + " " + maplet_y );

		//Log.w(TAG, "in onDraw " + draw_tile);
		if (center_maplet == null) {
			String bad = "NULL";
			int tx = cx - (int) myPaint.measureText(bad);
			canvas.drawText(bad, tx, cy, myPaint);
			return;
		}
		
		// XXX
		// eventually this could be a global thing.
		// 
		// For some levels this is a constant that could be
		// "wired" into the level initialization.
		// For others (like 24K), the X scale and pixel size
		// changes with latitude
		
		// maplet size in pixels
		px = center_maplet.width();
		py = center_maplet.height();
		
		// post scales for Location class
		scalex = level.maplet_dlong() / px;
		scaley = level.maplet_dlat() / py;
		
		// location of center spot in pixel counts
		// same sign as maplet_x/y
		offx = (int) (fx * px);
		offy = (int) (fy * py);
		
		// This puts the chosen location within the
		// center tile in the center of the canvas.
		ox = cx - (px - offx);
		oy = cy - (py - offy);
		
		// This centers the center tile.
		//ox = (cw - px )/2;
		//oy = (ch - py )/2;
		
		canvas.drawBitmap(center_maplet.map, ox, oy, null);
		
		//drawBox ( canvas, ox, oy, px, py );
		
		// counts from left to right
		nx1 = - ( ox + (px - 1)) / px;
		nx2 = + ( cw - (ox + px) + (px - 1)) / px;
		
		// counts from top to bottom
		ny1 = - ( oy + (py - 1)) / py;
		ny2 = + ( ch - (oy + py) + (py - 1)) / py;
		
		// kill loop that follows
		// (so we just display the center)
		// if ( true ) return;
		// nx1 = nx2 = 0;
		// ny1 = ny2 = 0;
		
		for ( int xx = nx1; xx <= nx2; xx++ ) {
			for ( int yy = ny1; yy <= ny2; yy++ ) {
				if ( xx == 0 && yy == 0 ) {
					continue;
				}
				Maplet extra = level.maplet_lookup ( maplet_x - xx, maplet_y - yy );
				
				ex = ox + xx * px;
				ey = oy + yy * py;
				
				if ( extra == null ) {
					// canvas.drawText("MAP", ex, ey, myPaint);
				    continue;
				}
				canvas.drawBitmap(extra.map, ex, ey, null);
			}
		}
		
		// Must do this after drawing all maplets or the lines
		// that draw the box outlines get hidden
		if ( show_boxes ) {
			for ( int xx = nx1; xx <= nx2; xx++ ) {
				for ( int yy = ny1; yy <= ny2; yy++ ) {
					ex = ox + xx * px;
					ey = oy + yy * py;
					drawBox ( canvas, ex, ey, px, py );
				}
			}
		}
		
		marker ( canvas, cx, cy );

		// Put debug info on top of the map.
		// for ( int i=0; i< msg_index; i++ ) {
		// 	canvas.drawText(msg[i], 100, 100 + i * 30, myPaint);
		// }
	}

	public void handle_move ( int dx, int dy ) {
		double delta_long;
		double delta_lat;
		
		delta_long = dx * scalex;
		delta_lat = dy * scaley;
		
		location.jog ( -delta_long,  delta_lat );
		
		invalidate();
	}
	
	private void dump_event ( MotionEvent e ) {
		int action = e.getAction();
		String msg = "Motion ";
		
		// Too much noise with these
		// if ( action == MotionEvent.ACTION_MOVE ) { return; }
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			msg += "Down ";
		} else if ( action == MotionEvent.ACTION_MOVE ) {
			msg += "Move ";
		} else if ( action == MotionEvent.ACTION_UP ) {
			msg += "Up " ;
		} else if ( action == MotionEvent.ACTION_POINTER_DOWN ) {
			msg += "Pointer Down ";
		} else if ( action == MotionEvent.ACTION_POINTER_UP ) {
			msg += "Pointer Up ";
		} else if ( action == MotionEvent.ACTION_POINTER_1_DOWN ) {
			msg += "Pointer 1 Down ";
		} else if ( action == MotionEvent.ACTION_POINTER_1_UP ) {
			msg += "Pointer 1 Up ";
		} else if ( action == MotionEvent.ACTION_POINTER_2_DOWN ) {
			msg += "Pointer 2 Down ";
		} else if ( action == MotionEvent.ACTION_POINTER_2_UP ) {
			msg += "Pointer 2 Up ";
		} else if ( action == MotionEvent.ACTION_POINTER_3_DOWN ) {
			msg += "Pointer 3 Down ";
		} else if ( action == MotionEvent.ACTION_POINTER_3_UP ) {
			msg += "Pointer 3 Up ";
		} else if ( action == MotionEvent.ACTION_CANCEL ) {
			msg += "Cancel ";
		} else {
			msg += "?" + action + " ";
		}
		
		int n = e.getPointerCount();
		msg += n;
		
		Log ( msg );
	}
	
	// None of what goes on in the following is documented well
	// Using the above event dumper and experimenting is the
	// only possible way to figure this out.
	// I never get a plain old ACTION_POINTER_UP/DOWN event,
	// but I do get the deprecated 1/2/3 flavor (go figure).
	// The MOVE events tell the story with accurate counts
	// and I do always get ACTION UP/DOWN wrapped around moves.
	
	// motion stuff ...
	private boolean have_first = false;
	private boolean have_first_dist = false;
	
	private int firstx, firsty;
	private int firstd;
	private int lastd;
	
	private int motion_count = 0;
	
	public void check_level_change () {
		Log2 ( "Transition: ", firstd, lastd );
		if ( lastd - firstd > 1000 ) {
			Level.down();
			invalidate();
		}
		if ( lastd - firstd < -1000 ) {
			Level.up();
			invalidate();
		}
	}
	
	@Override
	public boolean onTouchEvent ( MotionEvent e ) {
		
		int action = e.getAction();
		int x, y;
		
		// dump_event ( e );
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			//Log.w(TAG,"Touch - down");
			have_first = false;	// really !
			have_first_dist = false;
			return true;
		} else if ( action == MotionEvent.ACTION_UP ) {
			//Log.w(TAG,"Touch - up");
			
			// Common!  Transition 2 --> 0
			if ( have_first_dist ) {
				check_level_change ();
			}
			
			if ( motion_count > 0 ) {
				Log ( "Move " + motion_count + " ---> ZERO! (up)" );
				motion_count = 0;
			}
			
			have_first = false;
			have_first_dist = false;
			return true;
		} else if ( action == MotionEvent.ACTION_MOVE ) {
			int n = e.getPointerCount();
			
			if ( n != motion_count ) {
				Log ( "Move " + motion_count + " ---> " + n );
				motion_count = n;
			}
			
			if ( n > 2 ) {
				have_first = false;
				return true;
			}
			
			if ( n == 2 ) {
				int dx, dy, d;
				have_first = false;
				dx = (int) (e.getX(0) - e.getX(1));
				dy = (int) (e.getY(0) - e.getY(1));
				d = dx*dx + dy*dy;
				
				if ( ! have_first_dist ) {
					firstd = d;
					lastd = d;
					have_first_dist = true;
					return true;
				}
				lastd = d;
				return true;
			}
			
			// n == 1
			
			// transition from 2 to 1
			if ( have_first_dist ) {
				check_level_change ();
				have_first_dist = false;
			}
			
			x = (int) e.getX();
			y = (int) e.getY();
			//Log.w(TAG,"Touch - move: " + x + " " + y);
			if ( ! have_first ) {
				firstx = x;
				firsty = y;
				have_first = true;
				return true;
			}
			handle_move ( x-firstx, y-firsty );
			firstx = x;
			firsty = y;
			return true;
		}
		
		return super.onTouchEvent(e);
	}
	
}