package com.trebisky.atopo;

import android.support.v4.util.LruCache;

// This class fetches (and caches) maplets.
// There is one cache per level.

public class MapletCache extends LruCache<Integer,Maplet> {
	private final static int MAPLET_CACHE_SIZE = 5000;

	public MapletCache () {
		super (MAPLET_CACHE_SIZE);
	}
	
	// ensure both put and get use same key
	private int key ( int x, int y ) { return y*10000 + x; }
	
	// See if we get lucky and find it in the cache
	public Maplet fetch ( int x, int y ) {
		return (Maplet) get(key(x,y));
	}
		
	// No luck, fetch it from the file.
	public Maplet load ( int x, int y, tpqFile tpq, int idx ) {
		Maplet rv = new Maplet ( x, y, tpq, idx );
		if ( rv.map != null ) {
			put(key(x,y), rv);
			return rv;
		}
		return null;
	}
}

// THE END