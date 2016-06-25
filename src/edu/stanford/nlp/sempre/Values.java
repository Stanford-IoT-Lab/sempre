package edu.stanford.nlp.sempre;

import edu.stanford.nlp.sempre.thingtalk.ActionValue;
import edu.stanford.nlp.sempre.thingtalk.CommandValue;
import edu.stanford.nlp.sempre.thingtalk.ParamValue;
import edu.stanford.nlp.sempre.thingtalk.TriggerValue;
import fig.basic.LispTree;

// FIXME: Remove this dependency

/**
 * Utilities for Value.
 *
 * @author Percy Liang
 */
public final class Values {
  private Values() { }

  // Try to parse the LispTree into a value.
  // If it fails, just return null.
  public static Value fromLispTreeOrNull(LispTree tree) {
    if (tree.isLeaf())
      return null;
    String type = tree.child(0).value;
    if ("name".equals(type)) return new NameValue(tree);
    if ("boolean".equals(type)) return new BooleanValue(tree);
    if ("number".equals(type)) return new NumberValue(tree);
    if ("string".equals(type)) return new StringValue(tree);
    if ("list".equals(type)) return new ListValue(tree);
    if ("table".equals(type)) return new TableValue(tree);
    if ("description".equals(type)) return new DescriptionValue(tree);
    if ("url".equals(type)) return new UriValue(tree);
    if ("context".equals(type)) return new ContextValue(tree);
    if ("date".equals(type)) return new DateValue(tree);
    if ("error".equals(type)) return new ErrorValue(tree);
    if ("time".equals(type)) return new TimeValue(tree);
    // ThingTalk values
    if ("param".equals(type)) return new ParamValue(tree);
    if ("trigger".equals(type)) return new TriggerValue(tree);
    if ("action".equals(type)) return new ActionValue(tree);
    if ("command".equals(type)) return new CommandValue(tree);
    return null;
  }

  // Try to parse.  If it fails, throw an exception.
  public static Value fromLispTree(LispTree tree) {
    Value value = fromLispTreeOrNull(tree);
    if (value == null)
      throw new RuntimeException("Invalid value: " + tree);
    return value;
  }

  public static Value fromString(String s) { return fromLispTree(LispTree.proto.parseFromString(s)); }
}
