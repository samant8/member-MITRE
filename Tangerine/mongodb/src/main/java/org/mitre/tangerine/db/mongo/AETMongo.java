/*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *                   NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. FA8702-17-C-0001, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (MAY 2013)
 *
 * (c)2016-2017 The MITRE Corporation. All Rights Reserved.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */



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
    public void open() {
        mongoClient = new MongoClient(super.getHost(), super.getPort());
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
        collection.createIndex(Indexes.text("A.D"));
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
    public void close() {
        mongoClient.close();
    }


    @Override
    public List<String> getEntries() {
        return this.getElements();
    }

    @Override
    public void drop() {
        this.dropCollection();

    }

    @Override
    public void index() {
        this.indexCollection();
    }

    @Override
    public void access(String id) {
        this.accessCollection(id);

    }

    @Override
    public void update(Object data) throws AETException {
        if(data instanceof Document) {
            this.add((Document) data);
        } else {
            throw new AETException("The data to be inserted is the incorrect object type");
        }

    }
}
