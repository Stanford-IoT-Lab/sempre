package edu.stanford.nlp.sempre.ibase;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.thingtalk.*;

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

    public static QueryValue query(TypedStringValue project, DurationValue duration) {
        return new QueryValue(project.value, duration);
    }

    public static Value jsonOut(Value val) {
        Map<String, Object> json = new HashMap<>();
        json.put("query", ((QueryValue) val).query);
        json.put("duration", ((QueryValue) val).duration);
        return (new StringValue(Json.writeValueAsStringHard(json)));
    }

}