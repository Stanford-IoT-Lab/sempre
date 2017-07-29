package edu.stanford.nlp.sempre.ibase;

import edu.stanford.nlp.sempre.DateValue;
import edu.stanford.nlp.sempre.NumberValue;
import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by silei on 6/9/17.
 * Value for time duration
 */
public class DurationValue extends Value {

    private final DateValue start;
    private final DateValue end;
    private final NumberValue diff;
    private final Boolean isNow;

    public DurationValue(DateValue start, DateValue end) {
        this.start = getDate(start, null);
        this.end = getDate(end, null);
        this.isNow = false;
        this.diff = null;
    }

    public DurationValue(DateValue start) {
        this.start = getDate(start, null);
        this.end = DateValue.now();
        this.isNow = true;
        this.diff = null;
    }

    public DurationValue(NumberValue duration) {
        this.end = DateValue.now();
        this.diff = duration;
        this.start = null;
        this.isNow = true;
    }

    private DateValue getDate(DateValue date, NumberValue diff) {
        Calendar cal = Calendar.getInstance();
        if (date.year != -1)
            cal.set(Calendar.YEAR, date.year);
        if (date.month != -1)
            cal.set(Calendar.MONTH, date.month - 1); // Calendar month starts from 0
        if (date.day != -1)
            cal.set(Calendar.DATE, date.day);
        if (date.hour != -1)
            cal.set(Calendar.HOUR_OF_DAY, date.hour);
        if (date.minute != -1)
            cal.set(Calendar.MINUTE, date.minute);
        if (date.second != -1) {
            cal.set(Calendar.SECOND, (int) date.second);
            cal.set(Calendar.MILLISECOND, (int) ((date.second % 1) * 1000));
        }
        if (diff != null) {
            switch (diff.unit) {
                case "year":
                    cal.add(Calendar.YEAR, (int) diff.value);
                    break;
                case "month":
                    cal.add(Calendar.MONTH, (int) diff.value);
                    break;
                case "week":
                    cal.add(Calendar.WEEK_OF_YEAR, (int) diff.value);
                    break;
                case "day":
                    cal.add(Calendar.DATE, (int) diff.value);
                    break;
                case "h":
                    cal.add(Calendar.HOUR, (int) diff.value);
                    break;
                case "s":
                    cal.add(Calendar.SECOND, (int) diff.value);
                    break;
                default:
                    throw new RuntimeException("Invalid unit for date");
            }
        }
        return new DateValue(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND) + 0.001 * cal.get(Calendar.MILLISECOND)
                );
    }

    @Override
    public LispTree toLispTree() {
        LispTree tree = LispTree.proto.newList();
        tree.addChild("duration");
        if (this.start != null)
            tree.addChild(this.start.toLispTree());
        tree.addChild(this.end.toLispTree());
        if (this.diff != null)
            tree.addChild(this.diff.toLispTree());
        return tree;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> json= new HashMap<>();
        Map<String, Object> start= new HashMap<>();
        Map<String, Object> end = new HashMap<>();
        start.put("type", "tt.time.date");
        end.put("type", "tt.time.date");
        if (this.isNow)
            end.put("value", "tt.time.now");
        else
            end.put("value", this.end.toJson());

        if (this.start == null) {
            start.put("value", end.get("value"));
            start.put("diff", this.diff.toJson());
        } else {
            start.put("value", this.start.toJson());
        }
        json.put("type", "tt.time.duration");
        json.put("from", start);
        json.put("to", end);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DurationValue that = (DurationValue) o;
        if (this.isNow != that.isNow) return false;
        if (this.start != that.start) return false;
        if (this.end != that.end) return false;
        if (this.diff != that.diff) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
