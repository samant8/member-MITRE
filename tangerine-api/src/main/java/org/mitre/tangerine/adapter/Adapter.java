package org.mitre.tangerine.adapter;

import java.io.InputStream;
import java.util.logging.Level;

import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

public abstract class Adapter<T, V> {
	

	public abstract String getName();
	public abstract String getCanonicalName();
	
	
	
	public abstract ResponseModel adapt(String uuid, T data) throws AETException;
	public abstract ResponseModel updateDB(ResponseModel data, AETDatabase db) throws AETException;
	
	// TODO specify map file to use
	
	// TODO expose data map
	
	
    public InputStream loadFile(Object callerClass, String storedName) throws AETException {        
    	InputStream is = null;        
        try {
        	ClassLoader loader = Thread.currentThread().getContextClassLoader();
        	is = loader.getResourceAsStream(storedName);            
            try {
                if (is == null) {
                    throw new AETException(Level.SEVERE, "No Input Stream Found.", 1);
                }
            } catch (IllegalArgumentException e) {
                throw new AETException(Level.SEVERE, e.getMessage(), 2);
            }        
        } catch (AETException e) {
        	throw e;
        }
        return is;
    }
		
}
