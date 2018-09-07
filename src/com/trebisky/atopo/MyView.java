package com.trebisky.atopo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MyView extends View {

	private static final int NUM_MSG = 10;
	private static String[] msg = new String[NUM_MSG];
	private static int msg_index;

	private static String trouble_msg;
	
	private static final String TAG = "atopo";
	
	public static void Log ( String msg ) {
		Log.e ( TAG, msg );
	}
	
	public static void Log1 ( String msg, int a ) {
		Log.e ( TAG, msg + " " + a );
	}
	
	public static void Log2 ( String msg, int a, int b ) {
		Log.e ( TAG, msg + " " + a + " " + b );
	}
	
	public static void Log2d ( String msg, double a, double b ) {
		Log.e ( TAG, msg + " " + a + " " + b );
	}
	
	public static void Log3 ( String msg, int a, int b, int c ) {
		Log.e ( TAG, msg + " " + a + " " + b + " " + c );
	}
	
	public static void Log4 ( String msg, int a, int b, int c, int d ) {
		Log.e ( TAG, msg + " " + a + " " + b + " " + c + " " + d );
	}
	
	// degrees per pixel
	private double scalex, scaley;

	private static boolean hires = false;

	// defaults are appropriate for tablets
	private static int text_size = 20;
	private static int disp_height = 25;
	private static int disp_width = 200;

	// center cursor sizes
	private static int cur_small = 5;
	private static int cur_big = 10;
	private static int cur_circle = 12;
	
	// must call before instantiating
	public static void set_hires ( boolean val) {
		hires = val;
		if ( hires ) {
            text_size = 40;
            cur_small = 10;
            cur_big = 20;
            cur_circle = 24;
            disp_height = 50;
            disp_width = 400;
		} else {
            text_size = 20;
            cur_small = 5;
            cur_big = 10;
            cur_circle = 12;
            disp_height = 25;
            disp_width = 200;
		}

		// This would be good, but ...
		// to redo paint
		// init ();
	}
	
	private static int display_mode = 0;

	private Paint myPaint;
	private Paint myText;
	private Paint smooth_paint;

	// for lat/long display
	private Paint llbg_Paint;
	private Paint llfg_Paint;

	// shared by all initializers
	private void init() {

		// Using this for text yields hollow letters
		myPaint = new Paint();
		myPaint.setColor(Color.BLACK);
        myPaint.setTextSize(text_size);
		myPaint.setStyle(Paint.Style.STROKE);
		if ( hires )
		    myPaint.setStrokeWidth(2.0F);

		myText = new Paint();
		myText.setColor(Color.BLACK);
        myText.setTextSize(text_size);
		myText.setStyle(Paint.Style.FILL_AND_STROKE);
		if ( hires )
		    myText.setStrokeWidth(2.0F);


		/* setting this true gives somewhat slow bilinear */
		/* setting this false gives fast nearest neighbor */
		smooth_paint = new Paint();
		smooth_paint.setFilterBitmap(true);

		llbg_Paint = new Paint();
		llbg_Paint.setColor(Color.WHITE);
        llbg_Paint.setTextSize(text_size);
		llbg_Paint.setStyle(Paint.Style.FILL);
        //llbg_Paint.setStrokeWidth(10);

		llfg_Paint = new Paint();
		llfg_Paint.setColor(Color.BLUE);
        llfg_Paint.setTextSize(text_size);
		llfg_Paint.setStyle(Paint.Style.FILL);
	}
	
	private MainActivity boss;
	
	public MyView(Context context) {
		super(context);
		
		boss = (MainActivity) context;
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
	
	public static void set_display_mode ( int arg ) {
		display_mode = arg;
	}
	
	// these set the screen all blue and write messages in black text
	// on a phone, the messages are very tiny.
	public static void setmsg ( String arg ) {
		if ( msg_index >= NUM_MSG ) return;
		msg[msg_index++] = arg;
	}
	
	public static void onemsg ( String arg ) {
		setmsg ( arg );
	}
	
	public static void trouble ( String msg ) {
        trouble_msg = msg;
	}
	
	private void drawBox ( Canvas canvas, int ox, int oy, int px, int py ) {
		canvas.drawLine ( ox, oy, ox+px, oy, myPaint );
		canvas.drawLine ( ox, oy, ox, oy+py, myPaint );
		canvas.drawLine ( ox+px, oy, ox+px, oy+py, myPaint );
		canvas.drawLine ( ox, oy+py, ox+px, oy+py, myPaint );
	}
	
	private int marker_type;
	
	// call this to set the marker type
	// 0 is blank
	// 1 is small plus
	// 2 is big plus
	// 3 is big plus with circle
	public void marker_type ( int arg ) {
		marker_type = arg;
	}
	
	private void marker ( Canvas canvas, int x, int y ) {

		// type 0 is blank - no marker
		if ( marker_type == 1 ) {
			canvas.drawLine ( x-cur_small, y, x+cur_small, y, myPaint );
			canvas.drawLine ( x, y-cur_small, x, y+cur_small, myPaint );
		} else if ( marker_type == 2 ){
			canvas.drawLine ( x-cur_big, y, x+cur_big, y, myPaint );
			canvas.drawLine ( x, y-cur_big, x, y+cur_big, myPaint );
		} else if ( marker_type == 3 ){
			canvas.drawLine ( x-cur_big, y, x+cur_big, y, myPaint );
			canvas.drawLine ( x, y-cur_big, x, y+cur_big, myPaint );
			canvas.drawCircle ( x, y, cur_circle, myPaint );
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int ox, oy;
		int ex, ey;
		int cw, ch; // size of canvas
		int cx, cy; // center of canvas
		double fx, fy;
		
		int orig_px, orig_py;
		int px, py;
		int offx, offy;
		int nx1, nx2;
		int ny1, ny2;
		
		Rect src;
		Rect dst;
		
		Maplet center_maplet;
		
		final boolean show_boxes = false;
		
		// canvas size
		// on my phone (in portrait orientation)
		// I get 1080 wide, 1920 high
		cw = canvas.getWidth();
		ch = canvas.getHeight();

		// XXX - I read that the above is long deprecated and I
		// really should be doing the following
		// Display display = getWindowManager().getDefaultDisplay();
		// Point size = new Point();
		// display.getSize(size);
		// cw = size.x; 
		// ch = size.y;
		
		// canvas center
		cx = cw / 2;
		cy = ch / 2;
		
		// Sometimes this shows through
		// as narrow vertical blue lines
		// as we scroll into a region with
		// different pixel width for maplets.
		canvas.drawColor(Color.BLUE);
		
		/* Put this panic message in the center */
		if ( trouble_msg != null ) {
            canvas.drawText(trouble_msg, cx-100, cy, myText);
            return;
		}

		// A great way to do some debugging
		// These rattle down from the top left
		for ( int i=0; i< msg_index; i++ ) {
			Log ( "Msg: " + msg[i] );
			canvas.drawText(msg[i], 10, 100+i*40, myText);
		}
		
		// No actual map display when showing these messages
		if ( msg_index > 0 ) return;
		
		// Is there a map here ?
		if ( ! Level.cur_check_map () ) {
			canvas.drawText("No Map !!", cx-100, cy, myText);
			return;
		}
		
		// world maplet x/y from lower right
		// X increasing to left, Y increasing up.
		int maplet_x = Level.cur_maplet_x();
		int maplet_y = Level.cur_maplet_y();
		//Log ( "Draw: " + map + " " + maplet_x + " " + maplet_y );
		
		fx = Level.cur_fx();
		fy = Level.cur_fy();
		
		// XXX XXX
		// We really only need to get this here
		// in this routine so that we have access
		// to values for px, py, someday we will
		// get those at TPQ initialization time
		// (maybe from the header), in which case
		// we will get rid of this here AND let
		// the loop below handle the 0,0 case
		
		// center_maplet = level.maplet_lookup ( sheet_x, sheet_y );
		center_maplet = Level.cur_maplet_lookup ( maplet_x, maplet_y );
		// Log ( "View mx,my A " +maplet_x + " " + maplet_y );

		//Log.w(TAG, "in onDraw " + draw_tile);
		if (center_maplet == null) {
			String bad = "NULL";
			int tx = cx - (int) myPaint.measureText(bad);
			canvas.drawText(bad, tx, cy, myText);
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
		orig_px = center_maplet.width();
		orig_py = center_maplet.height();
		// px = orig_px;
		// py = orig_py;
		px = (int) (orig_px * Level.get_zoom());
		py = (int) (orig_py * Level.get_zoom());
		
		// update values for motion scaling
		scalex = Level.maplet_dlong() / px;
		scaley = Level.maplet_dlat() / py;
		
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
		
		/* XXX - you would think you would want to
		 * subtract one from the following values, but
		 * that yields a thin blue line.
		 */
		src = new Rect ( 0, 0, orig_px, orig_py );
		dst = new Rect ( ox, oy, ox+px, oy+py );
		
		// canvas.drawBitmap(center_maplet.map, ox, oy, null);
		canvas.drawBitmap(center_maplet.map, src, dst, smooth_paint );
		
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
				Maplet extra = Level.cur_maplet_lookup ( maplet_x - xx, maplet_y - yy );
				
				ex = ox + xx * px;
				ey = oy + yy * py;
				
				if ( extra == null ) {
					// canvas.drawText("MAP", ex, ey, myText);
				    continue;
				}
				// canvas.drawBitmap(extra.map, ex, ey, null);
				src = new Rect ( 0, 0, extra.width(), extra.height() );
				dst = new Rect ( ex, ey, ex+px, ey+py );
				canvas.drawBitmap( extra.map, src, dst, smooth_paint );
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
		// 	canvas.drawText(msg[i], 100, 100 + i * 30, myText);
		// }
		
		// as near as I can tell, the android canvas origin
		// is at the top left
		// left, top, right, bottom

		if ( display_mode > 0 ) {
		    //Rect llbox = new Rect ( cx, cy, cx+100, cy+100 );
		    Rect llbox = new Rect ( 0, ch-disp_height, cw, ch );
		    canvas.drawRect ( llbox, llbg_Paint );
		    int ypos = hires ? 100 : 500;
		    if ( display_mode > 1 ) {
		        canvas.drawText( Level.cur_long_f() , ypos, ch-5, llfg_Paint);
		        canvas.drawText( Level.cur_lat_f() , ypos+disp_width, ch-5, llfg_Paint);
		    } else {
		        canvas.drawText( Level.cur_long_dms() , ypos, ch-5, llfg_Paint);
		        canvas.drawText( Level.cur_lat_dms() , ypos+disp_width, ch-5, llfg_Paint);
		    }
		    ypos += 2* disp_width;
		    canvas.drawText( Level.cur_alt_string() , ypos, ch-5, llfg_Paint);
		}
	}

	public void handle_move ( int dx, int dy ) {
		double delta_long;
		double delta_lat;
		
		delta_long = dx * scalex;
		delta_lat = dy * scaley;
		
		Level.jogpos ( -delta_long,  delta_lat );
		
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
		// Log2 ( "Transition: ", firstd, lastd );
		if ( lastd - firstd > 1000 ) {
			if ( Level.down() )
				invalidate();
		}
		if ( lastd - firstd < -1000 ) {
			if ( Level.up() )
				invalidate();
		}
	}
	
	private int last_touch = 0;
	private int touch_count = 0;
	
	// Called on ACTION_UP to see if we
	// have a double tap to toggle the gps
	//
	// The taps in a double tap are usually
	// about 150 milliseconds apart.
	private void check_touch ( MotionEvent e ) {
		
		// We could use t2-t1 to detect a long touch
		int t1 = (int) e.getDownTime ();
		int t2 = (int) e.getEventTime ();
		
		// time since last touch.
		int dt = t2 - last_touch;
		last_touch = t2;
		
		//Log3 ( "Touch: ", t2, dt,  t2-t1 );
		
		if ( dt > 300 ) {
			touch_count = 1;
			Log3 ( "Touch: ", dt,  t2-t1, touch_count );
			return;
		}
		touch_count++;
		Log3 ( "Touch: ", dt,  t2-t1, touch_count );
	}
	
	// Called from the timer task
	//  every 250 ms
	public void motion_tick () {
		if ( touch_count < 1 ) return;
		
		long wait = SystemClock.uptimeMillis() - last_touch;
		if ( wait < 300 ) return;
		
		if ( touch_count == 2 ) {
			//Log ( "handle double click" );
			// get rid of this 9-8-2018 - use menu instead.
			// boss.post_double_click ();
		}
		touch_count = 0;
	}
	
	private int down_x, down_y;
	
	@Override
	public boolean onTouchEvent ( MotionEvent e ) {
		
		int action = e.getAction();
		int x, y;
		int dx, dy;
		int d; 
		
		// dump_event ( e );
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			//Log.w(TAG,"Touch - down");
			have_first = false;	// really !
			have_first_dist = false;
			
			down_x = (int) e.getX();
			down_y = (int) e.getY();
			return true;
		} else if ( action == MotionEvent.ACTION_UP ) {
			//Log.w(TAG,"Touch - up");
			
			dx = (int) (e.getX() - down_x);
			dy = (int) (e.getY() - down_y);
			d = dx*dx + dy*dy;
			
			if ( d < 1000 )
				check_touch ( e );
			else
				Log ( "Touch: ignoring (was move)" );
			
			// Common!  Transition 2 --> 0
			if ( have_first_dist ) {
				check_level_change ();
			}
			
			if ( motion_count > 0 ) {
				// Log ( "Move " + motion_count + " ---> ZERO! (up)" );
				motion_count = 0;
			}
			
			have_first = false;
			have_first_dist = false;
			return true;
		} else if ( action == MotionEvent.ACTION_MOVE ) {
			int n = e.getPointerCount();
			
			if ( n != motion_count ) {
				// Log ( "Move " + motion_count + " ---> " + n );
				motion_count = n;
			}
			
			if ( n > 2 ) {
				have_first = false;
				return true;
			}
			
			if ( n == 2 ) {
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
			// (note - for some reason,
			//   the usual transition is 2 to 0)
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

// THE END
