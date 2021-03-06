package edu.stanford.nlp.sempre.thingtalk;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

public class TypedStringValue extends Value {
  public final String type;
  public final String value;
  public final String display;

  public TypedStringValue(String type, String value) {
    this.type = type;
    this.value = value;
    this.display = null;
  }

  public TypedStringValue(String type, String value, String display) {
    this.type = type;
    this.value = value;
    this.display = display;
  }

  public TypedStringValue(LispTree tree) {
    this.type = tree.child(1).value;
    this.value = tree.child(2).value;
    this.display = tree.child(3).value;
  }

  @Override
  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("typedstring");
    tree.addChild(type);
    tree.addChild(value);
    tree.addChild(display);
    return tree;
  }

  public String getType() {
    return type;
  }

  @Override
  public Map<String, Object> toJson() {
    Map<String, Object> json = new HashMap<>();
    json.put("value", value);
    if (display != null)
      json.put("display", display);
    return json;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + type.hashCode();
    result = prime * result + value.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TypedStringValue other = (TypedStringValue) obj;
    if (!type.equals(other.type))
      return false;
    if (!value.equals(other.value))
      return false;
    return true;
  }
}
