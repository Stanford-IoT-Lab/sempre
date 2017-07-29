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
    public final String op;

    public QueryValue(String query, DurationValue duration, String op) {
        this.query = query;
        this.duration = duration;
        this.op = op;
    }

    @Override
    public LispTree toLispTree() {
        LispTree tree = LispTree.proto.newList();
        tree.addChild("query");
        tree.addChild(this.query);
        tree.addChild(this.duration.toLispTree());
        if (op != null)
            tree.addChild(this.op);
        return tree;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> json= new HashMap<>();
        json.put("query", this.query);
        json.put("duration", this.duration.toJson());
        if (op != null)
            json.put("op", this.op);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryValue that = (QueryValue) o;
        if (this.query != that.query) return false;
        if (this.duration != that.duration) return false;
        if (this.op != that.op) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
