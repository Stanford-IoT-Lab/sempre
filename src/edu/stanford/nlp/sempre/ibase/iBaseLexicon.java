package edu.stanford.nlp.sempre.ibase;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import edu.stanford.nlp.sempre.StringValue;
import edu.stanford.nlp.sempre.ValueFormula;
import edu.stanford.nlp.sempre.thingtalk.AbstractLexicon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by silei on 7/23/17.
 */
public class iBaseLexicon extends AbstractLexicon<StringValue> {
    private final MongoCollection ibase;

    public iBaseLexicon() {
        this.ibase = iBaseDatabase.getCollection();
    }

    @Override
    protected Collection<Entry<StringValue>> doLookup(String rawPhrase) {
        if (rawPhrase == null)
            return Collections.emptySet();

        Collection<Entry<StringValue>> types = new LinkedList<>();
        MongoCursor cursor = this.ibase.distinct("type", String.class).iterator();
        while (cursor.hasNext()) {
            String next = (String) cursor.next();
            types.add(new Entry<>("TYPE",
                    new ValueFormula<>(new StringValue(next)),
                    next));
        }
        return types;
    }
}
