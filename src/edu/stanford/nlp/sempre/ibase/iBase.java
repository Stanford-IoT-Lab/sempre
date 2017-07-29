package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.thingtalk.TypedStringValue;

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
        return new DurationValue(start);
    }

    public static DurationValue measureDurationCast(NumberValue duration) {
        return new DurationValue(duration);
    }

    public static QueryValue query(Value project, DurationValue duration) {
        if (project instanceof TypedStringValue)
            return new QueryValue(((TypedStringValue) project).value, duration, null);
        else if (project instanceof StringValue)
            return new QueryValue(((StringValue) project).value, duration, null);
        else
            return null;
    }

    public static QueryValue query(StringValue op, Value project, DurationValue duration) {
        if (project instanceof TypedStringValue)
            return new QueryValue(((TypedStringValue) project).value, duration, op.value);
        else if (project instanceof StringValue)
            return new QueryValue(((StringValue) project).value, duration, op.value);
        else
            return null;
    }

    public static Value jsonOut(Value val) {
        return new StringValue(Json.writeValueAsStringHard(val.toJson()));
    }

}