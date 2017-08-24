package org.mitre.tangerine.adapter;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.junit.Test;
import org.mitre.tangerine.adapter.Adapter;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

public class AdapterTest {
	
	@Test
	public void loadFile() {
		Adapter<?,?> mock = new MockAdapter();
		try {
			assert(mock.adapt(null, null).getErrorMessage().equals("TEST"));
		} catch (AETException e) {
			fail();
		}
	}
	
	@Test
	public void failFile() {
		Adapter<?,?> mock = new FailAdapter();
		try {
			mock.adapt(null, null).getErrorMessage().equals("TEST");
		} catch (AETException e) {
			assert(e.getMessage().contains("No Input Stream Found"));
		}
	}
	
	@Test
	public void updateDB() {
		MockAdapter mock = new MockAdapter();
		try {
			mock.updateDB(null, null);
		} catch (AETException e) {
			assert(e.getMessage().contains("unimplemented"));
		}

	}

	public class MockAdapter extends Adapter<Object, Object> {
		
		public MockAdapter() {
			super();
		}

		public ResponseModel adapt(String id, Object m) throws AETException {
			BufferedReader ready = new BufferedReader(new InputStreamReader(this.loadFile(this.getClass(), "data-map.json")));
			ResponseModel rep = new ResponseModel();
			rep.setError(true);
			try {
				rep.setErrorMessage(ready.readLine());
			} catch (IOException e) {
				throw new AETException(Level.SEVERE, "couldn't read data map", 0);
			}
			return rep;
		}

		@Override
		public ResponseModel updateDB(ResponseModel data, AETDatabase db) throws AETException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getCanonicalName() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	public class FailAdapter extends Adapter<Object, Object> {
		
		public FailAdapter() {
			super();
			// TODO Auto-generated constructor stub
		}

		public ResponseModel adapt(String id, Object m) throws AETException {
			BufferedReader ready = new BufferedReader(new InputStreamReader(this.loadFile(this.getClass(), "does-not-exist.json")));
			ResponseModel rep = new ResponseModel();
			rep.setError(true);
			try {
				rep.setErrorMessage(ready.readLine());
			} catch (IOException e) {
				throw new AETException(Level.SEVERE, "couldn't read data map", 0);
			}
			return rep;
		}

		@Override
		public ResponseModel updateDB(ResponseModel data, AETDatabase db) throws AETException {
			throw new AETException(Level.INFO, "unimplemented", 0);
			
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getCanonicalName() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
