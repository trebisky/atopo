package com.trebisky.atopo;

// XXX which version do I really want?
// Apparently the v4 flavor is older and
// compatible with older android releases, the other one
// without the v4 requires at least android release 12.
// import android.util.LruCache;
import android.support.v4.util.LruCache;

// XXX - Using the LruCache "properly" involves the use of what are
// called "Java Generics" (the wacky stuff between angle brackets),
// which I have not yet mastered.

public class FileCache extends LruCache<String, tpqFile> {
		
	private final static int FILE_CACHE_SIZE = 500;
			
	public FileCache () {
		super (FILE_CACHE_SIZE);
	}
	
	public tpqFile fetch ( String who ) {
		String path;
		
		// Log.w ( "atopo", "cache get: " + who );
		tpqFile rv = (tpqFile) get(who);
		if ( rv != null )
			return rv;
		
		path = Level.base_path() + "/" + who + ".tpq";
		//Log.w ( "atopo", "cache path: " + path );
		
		rv = new tpqFile ( path );
		if ( rv.isvalid() ) {
			put( who, rv );
		}
		// if not valid, let caller discover
		// a null pointer will blow him up
		// if he tests via isvalid()
		return rv;
	}

}

// THE END