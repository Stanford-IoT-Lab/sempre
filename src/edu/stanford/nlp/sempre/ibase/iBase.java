package edu.stanford.nlp.sempre.ibase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

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

    public static Value jsonOutFilters(Value val) {
        // e.g. {"date": {"$lt": {"type":"tt.time", "value":{"relative": "tt.time.now"}}}}
        Map<String, Object> json = new HashMap<>();
        json.put("date", val.toJson());
        return (new StringValue(Json.writeValueAsStringHard(json)));
    }

    public static Value jsonOutFields() {
        // eg. {weight: 1, _id: 0}
        Map<String, Object> json = new HashMap<>();
        return (new StringValue(Json.writeValueAsStringHard(json)));
    }

}