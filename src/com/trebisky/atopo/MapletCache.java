package com.trebisky.atopo;

import android.support.v4.util.LruCache;

// This class fetches (and caches) maplets.
// There is one cache per level.

public class MapletCache extends LruCache<Integer,Maplet> {
	private final static int MAPLET_CACHE_SIZE = 5000;

	// When I added this, I start getting endless GC messages,
	// and things got even worse when I added the entryRemoved method,
	// the log just chatters away when a static map is displayed.
	//@Override
    //protected int sizeOf(Integer key, Maplet maplet) {
        // The cache size will be measured in kilobytes rather than
        // number of items.
    //    return maplet.map.getByteCount() / 1024;
    //}
	
	// I am unsure about how to properly handle this.
	// It seems to me though if entries are getting evicted and
	// I am not freeing memory, we will get a bad memory leak.
	//@Override
    //protected void entryRemoved(boolean evicted, Integer key, Maplet m_old, Maplet m_new ) {
	//	MyView.Log ( "MapletCache entry removed: " + key );
	//}


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