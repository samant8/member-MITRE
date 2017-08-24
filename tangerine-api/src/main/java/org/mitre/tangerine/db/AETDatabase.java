package org.mitre.tangerine.db;

import java.util.List;

import org.mitre.tangerine.exception.AETException;

public abstract class AETDatabase {

	private String host, db;
	private int port;
	
	// TODO user, pass
	
	public AETDatabase(String host, int port, String db){
		this.host = host;
		this.port = port;
		this.db   = db;
	}
	
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getDb() {
		return db;
	}

	public abstract void openConnection();
	public abstract void accessDatabase();
	public abstract void accessSelection(String id);
	public abstract void updateSelection(Object data) throws AETException;
	public abstract void dropSelection();
	public abstract void indexSelection();
	public abstract void closeConnection();
	public abstract List<String> getEntries() throws AETException;

}
