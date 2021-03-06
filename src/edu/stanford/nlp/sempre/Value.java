package edu.stanford.nlp.sempre;

import java.util.Comparator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import fig.basic.LispTree;
import fig.basic.LogInfo;

/**
 * Values represent denotations (or partial denotations).
 *
 * @author Percy Liang
 */
public abstract class Value {

  public abstract LispTree toLispTree();

  // Print using LogInfo.
  public void log() { LogInfo.logs("%s", toString()); }

  @Override
@JsonValue
  public String toString() { return toLispTree().toString(); }

  @JsonCreator
  public static Value fromString(String str) {
    return Values.fromLispTree(LispTree.proto.parseFromString(str));
  }

  public abstract Map<String, Object> toJson();
  @Override public abstract boolean equals(Object o);
  @Override public abstract int hashCode();

  public static class ValueComparator implements Comparator<Value> {
    @Override
    public int compare(Value o1, Value o2) {
      return o1.toString().compareTo(o2.toString());
    }
  }
}
