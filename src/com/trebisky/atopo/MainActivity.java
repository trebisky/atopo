package com.trebisky.tpqreader;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

	// Tucson, Arizona
	// private final double LONG_START = -110.94;
	// private final double LAT_START = 32.27;
	
	// Grand Canyon, Tower of Ra
	// private final double LONG_START = -112.203;
	// private final double LAT_START = 36.141;
	
	// Grand Canyon, Horseshoe Mesa
	private final double LONG_START = -111.975;
	private final double LAT_START = 36.024;
	
	// private final int MAP_WIDTH = 1280;
	// private final int MAP_HEIGHT = 800;

	// Entire US at level 1  12x8 = 96 maplets
	// private final String tpq_file = "/storage/sdcard1/us1map1.tpq";
	// private final String tpq_file = "/storage/sdcard1/topo/us1map1.tpq";
	
	// Entire US at level 2  59x26 = 1534 maplets
	// private final String tpq_file = "/storage/sdcard1/us1map2.tpq";
	// private final String tpq_file = "/storage/sdcard1/topo/us1map2.tpq";
	
	private final String tpq_dir = "/storage/sdcard1/topo/l5";
	private final String tpq_file = "/storage/sdcard1/topo/l5/n36112b2.tpq";
	
	private final int delay = 2 * 1000;
	
	private MyView view;

	private static Handler handle = new Handler();

	// This runs in its own thread, so no UI updates here.
	class tickTask extends TimerTask {
		@Override
		public void run() {
			view.nextFile();

			// We invalidate the view, which forces an onDraw()
			handle.post(new Runnable() {
				public void run() {
					view.invalidate();
				}
			});
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// boolean isPortrait = getResources().getConfiguration().orientation ==
		// Configuration.ORIENTATION_PORTRAIT;
		// int frameBufferWidth = isPortrait ? MAP_HEIGHT : MAP_WIDTH;
		// int frameBufferHeight = isPortrait ? MAP_WIDTH : MAP_HEIGHT;

		// Bitmap frameBuffer = Bitmap.createBitmap(frameBufferWidth,
		// frameBufferHeight, Config.RGB_565);

		// float scaleX = (float) frameBufferWidth
		// / getWindowManager().getDefaultDisplay().getWidth();
		// float scaleY = (float) frameBufferHeight
		// / getWindowManager().getDefaultDisplay().getHeight();

		view = new MyView(this);
		// view.nomaps();
		
		view.setDir(tpq_dir);
		view.setTPQ(tpq_file);
		view.setMap(LONG_START,LAT_START);
		view.setLoc(LONG_START,LAT_START);
		
		setContentView(view);

		Timer timer = new Timer();
		timer.schedule(new tickTask(), 0, delay);

		// PowerManager powerManager = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
		// "Sabino");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}