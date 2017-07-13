package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.DateValue;
import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by silei on 6/9/17.
 * Value for time duration
 */
public class DurationValue extends Value {

    public final DateValue start;
    public final DateValue end;

    public DurationValue(DateValue start, DateValue end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public LispTree toLispTree() {
        return null;
    }

    @Override
    public Map<String, Object> toJson() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
