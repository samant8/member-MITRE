package org.mitre.tangerine.db.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Indexes;

import org.mitre.tangerine.db.nosql.AETDocument;
import org.mitre.tangerine.exception.AETException;

public class AETMongo extends AETDocument<Document>  {

	private MongoClient mongoClient;
	private MongoDatabase database;
	private MongoCollection<Document> collection;

	@Deprecated
	public AETMongo() {
		super("localhost", 27017, "mydb");
	}

	public AETMongo(String host, int port, String db) {
		super(host, port, db);
	}

	// Connect to database
	@Override
	public void accessDatabase() {
		database = mongoClient.getDatabase(super.getDb());
	}

	// Print number of elements in collection
	@Override
	public long countElements() {
		return collection.count();
	}

	@Override
	public void indexCollection() {
		BasicDBObject index = new BasicDBObject();
		index.put("A.S", 1);
		index.put("A.P", 1);
		collection.createIndex(index);
		index = new BasicDBObject();
		index.put("A.O", 1);
		index.put("A.P", 1);
		collection.createIndex(index);
		collection.createIndex(Indexes.text("D"));
	}

	@Override
	public void accessCollection(String collection) {
		this.collection = database.getCollection(collection);

	}

	@Override
	public void dropCollection() {
		this.collection.drop();
	}

	@Override
	public List<String> getCollections() {
		MongoIterable<String> colls = database.listCollectionNames();
		return colls.into(new ArrayList<String>());
	}

	public void add(Document doc) {
		collection.insertOne(doc);
	}

	public Document remove(Document doc) {
		return this.collection.findOneAndDelete(doc);
	}

	@Override
	public List<String> getElements() {
		ArrayList<String> list = new ArrayList<String>();
		MongoCursor<Document> cursor = collection.find().iterator();
		while (cursor.hasNext()) {
			list.add(cursor.next().toJson());
		}
		cursor.close();
		return list;
	}

	public List<String> getElements(int x) {
		ArrayList<String> list = new ArrayList<String>();
		MongoCursor<Document> cursor = collection.find().limit(x).iterator();
		while (cursor.hasNext()) {
			list.add(cursor.next().toJson());
		}
		cursor.close();
		return list;
	}

	@Override
	public void openConnection() {
		mongoClient = new MongoClient(super.getHost(), super.getPort());
	}

	@Override
	public void closeConnection() {
		mongoClient.close();
	}


	@Override
	public List<String> getEntries() throws AETException {
		return this.getElements();
	}

	@Override
	public void dropSelection() {
		this.dropCollection();
		
	}

	@Override
	public void indexSelection() {
		this.indexCollection();	
	}

	@Override
	public void accessSelection(String id) {
		this.accessCollection(id);
		
	}

	@Override
	public void updateSelection(Object data) throws AETException {
		if(data instanceof Document){
			this.add((Document) data);		
		} else {
			throw new AETException(Level.WARNING, "The data to be inserted is the incorrect object type", 0);
		}
		
	}
}
