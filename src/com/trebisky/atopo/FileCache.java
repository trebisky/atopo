package com.trebisky.atopo;

import android.util.Log;
// XXX which one do I really want?
// Apparently the v4 flavor is older and
// compatible with older android releases, the other one
// without the v4 requires at least android release 12.
// import android.util.LruCache;
import android.support.v4.util.LruCache;

public class FileCache {
		
	private int FILE_CACHE_SIZE = 500;
	private LruCache cache;
			
	public FileCache () {
		cache = new LruCache<String, tpqFile>(FILE_CACHE_SIZE);
	}
	
	public tpqFile get ( String who ) {
		String path;
		
		// Log.w ( "atopo", "cache get: " + who );
		// Log.e ( "atopo", "hamster" );
		tpqFile rv = (tpqFile) cache.get(who);
		if ( rv != null )
			return rv;
		
		path = Level.base_path() + "/" + who + ".tpq";
		//Log.w ( "atopo", "cache path: " + path );
		
		rv = new tpqFile ( path );
		if ( rv.isvalid() ) {
			cache.put( who, rv );
			return rv;
		}
		return null;
	}

}

// THE END