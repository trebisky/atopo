package com.trebisky.atopo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class tpqFile {

	private RandomAccessFile rfile;
	private long file_size;
	
	private int[] index;
	private int[] size;
	private int n_index;
	
	// The following values are extracted from
	// the header.
	
	// map limits in degrees
	private double north, south, east, west;
	
	// number of maplets in this file (and layout)
	private int num_long, num_lat;
	
	// number of pixels in each maplet
	private int num_pixels_long, num_pixels_lat;
	
	public tpqFile ( String path ) {

		File tpqfile;

		try {
		    tpqfile = new File(path);
		    file_size = tpqfile.length();
		    rfile = new RandomAccessFile(tpqfile, "r");
		} catch (FileNotFoundException e) {
		    // e.printStackTrace();
			n_index = 0;
			return;
		}

		try {
		    read_header();
		    load_index();
		} catch (IOException e) {
		    // e.printStackTrace();
			n_index = 0;
			return;
		}
		
		// Log.w ( TAG, "num_long = " + num_long );
		// Log.w ( TAG, "num_lat = " + num_lat );
		// Log.w ( TAG, "n_index = " + n_index );

		// Log.w ( TAG, "memory limit: " + Runtime.getRuntime().maxMemory() );
		// Log.w ( TAG, "memory limit: " + Runtime.getRuntime().maxMemory() );
	}
	
	public void close () {
		try {
			rfile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}
	
	public boolean isvalid () {
		return n_index > 0;
	}
	
	public int offset ( int idx ) {
		if ( n_index <= 0 ) return 0;
		if ( idx < 0 || idx >= n_index ) return 0;
		return index[idx];
	}
	
	public int size ( int idx ) {
		return size[idx];
	}
	
	public RandomAccessFile rfile () {
		return rfile;
	}
	
	public double south () {
		return south;
	}
	public double north () {
		return north;
	}
	public double east () {
		return east;
	}
	public double west () {
		return west;
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
	
	// NOTE - XXX
	// all the jpeg scanning is needless given that the header
	// hold the maplet nx,my that we can just to compute the
	// index size.
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
		
		west = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		north = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		east = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		south = Double.longBitsToDouble ( Long.reverseBytes( rfile.readLong() ));
		
		// setmsg ( "West: " + west );
		// setmsg ( "East: " + east );
		
		rfile.skipBytes(456);
		
		// gives number of maplets in the file.
		num_long = Integer.reverseBytes(rfile.readInt());
		num_lat = Integer.reverseBytes(rfile.readInt());
	}
	
	public int num_long () {
		return num_long;
	}
	
	public int num_lat () {
		return num_lat;
	}
	
	public int num_pixels_long () {
		return num_pixels_long;
	}
	
	public int num_pixels_lat () {
		return num_pixels_lat;
	}
}

// THE END