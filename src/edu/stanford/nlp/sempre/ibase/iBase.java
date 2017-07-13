package edu.stanford.nlp.sempre.ibase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;

import edu.stanford.nlp.sempre.*;

/**
 * Created by silei on 6/9/17.
 * Value for time duration
 */
public final class iBase {
    public static DurationValue durationBetween(DateValue start, DateValue end) {
        DurationValue duration = new DurationValue(start, end);
        return duration;
    }

    public static DurationValue durationBefore(DateValue start) {
        return durationBetween(start, DateValue.now());
    }

    public static DurationValue duractionAfter(DateValue end) {
        return durationBetween(DateValue.now(), end);
    }

}