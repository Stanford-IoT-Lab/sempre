package edu.stanford.nlp.sempre;

import java.util.List;

import edu.stanford.nlp.sempre.Derivation.Cacheability;

public class QueryCache extends GenericObjectCache<String, List<Derivation>> {
	public static final long LEXICON_CACHE_AGE = 1000 * 3600 * 1; // cache lexicon lookups for 1 hour

	public QueryCache(int nBuckets) {
		super(nBuckets);
	}

	public void store(String query, List<Derivation> derivations) {
		// be conservative in cacheability
		Cacheability cache = Cacheability.CACHEABLE;
		for (Derivation d : derivations)
			cache = cache.meet(d.cache);
		long expires;
		switch (cache) {
		case CACHEABLE:
			expires = Long.MAX_VALUE;
			break;
		case LEXICON_DEPENDENT:
			expires = System.currentTimeMillis() + LEXICON_CACHE_AGE;
			break;
		default:
			expires = 0;
		}
		super.store(query, derivations, expires);
	}
}
