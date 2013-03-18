package com.trebisky.atopo;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Maplet {
	
	Bitmap map;
	
	int x, y;
	
	// I am not sure this is an optimization
	int width, height;
	
	public Maplet ( int _x, int _y, tpqFile tpq, int idx ) {
		
		x = _x;
		y = _y;
		
		map = loadBitmap ( tpq, idx );
		
		if ( map == null ) {
			MyView.Log ( "Maplet new: bad xy = " + x + "  " + y);
			return;
		}
		
		width = map.getWidth();
		height = map.getHeight();
	}
	
	private Bitmap loadBitmap ( tpqFile tpq, int idx) {
		
		byte [] jpeg_data = tpq.read_jpeg ( idx );
		if ( jpeg_data == null )
			return null;
		
		// Don't know why, but the following always returned null
		// myBitmap = BitmapFactory.decodeFileDescriptor ( rfile.getFD() );

		return BitmapFactory.decodeByteArray(jpeg_data, 0, jpeg_data.length );
	}
	
	public int width () {
		return width;
	}
	
	public int height () {
		return height;
	}
}

// THE END
