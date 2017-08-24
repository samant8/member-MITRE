package org.mitre.tangerine.db.nosql;

import java.util.List;

import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;


public abstract class AETDocument<M> extends AETDatabase {

	public AETDocument(String host, int port, String db) {
		super(host, port, db);
	}

	public abstract void accessCollection(String collection) throws AETException;
	public abstract void dropCollection() throws AETException;
	public abstract void indexCollection() throws AETException;
	public abstract long countElements() throws AETException;		
	public abstract List<String> getCollections() throws AETException;	
	public abstract List<String> getElements() throws AETException;	
	
}
