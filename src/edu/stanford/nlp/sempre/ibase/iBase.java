package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.thingtalk.TypedStringValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by silei on 6/9/17.
 * Functions for supporting ibase
 *
 */
public final class iBase {
    public static DurationValue measureDurationCast(DateValue start, DateValue end) {
        return new DurationValue(start, end);
    }

    public static DurationValue measureDurationCast(DateValue start) {
        return new DurationValue(start, DateValue.now());
    }

    public static DurationValue measureDurationCast(NumberValue duration) {
        return new DurationValue(duration);
    }

    public static QueryValue query(Value project, DurationValue duration) {
        if (project instanceof TypedStringValue)
            return new QueryValue(((TypedStringValue) project).value, duration);
        else if (project instanceof StringValue)
            return new QueryValue(((StringValue) project).value, duration);
        else
            return null;
    }

    public static Value jsonOut(Value val) {
        Map<String, Object> json = new HashMap<>();
        json.put("query", ((QueryValue) val).query);
        json.put("duration", ((QueryValue) val).duration);
        return (new StringValue(Json.writeValueAsStringHard(json)));
    }

}