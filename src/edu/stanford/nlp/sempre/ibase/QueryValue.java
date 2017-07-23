package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by silei on 7/23/17.
 */
public class QueryValue extends Value {
    public final String query;
    public final DurationValue duration;

    public QueryValue(String query, DurationValue duration) {
        this.query = query;
        this.duration = duration;
    }

    @Override
    public LispTree toLispTree() {
        LispTree tree = LispTree.proto.newList();
        tree.addChild("query");
        tree.addChild(this.query);
        tree.addChild(this.duration.toLispTree());
        return tree;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> json= new HashMap<>();
        json.put("query", this.query);
        json.put("duration", this.duration);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryValue that = (QueryValue) o;
        if (this.query != that.query) return false;
        if (this.duration != that.duration) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
