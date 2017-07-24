package edu.stanford.nlp.sempre.ibase;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Created by silei on 7/23/17.
 */
public class iBaseDatabase{
    private static MongoClient client = new MongoClient();
    private static MongoDatabase db = client.getDatabase("local");
    private static MongoCollection<Document> ibase = db.getCollection("ibase");

    public static MongoCollection getCollection() {
        return ibase;
    }

}
