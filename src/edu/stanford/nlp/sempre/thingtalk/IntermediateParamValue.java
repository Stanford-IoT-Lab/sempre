package edu.stanford.nlp.sempre.thingtalk;

import java.util.Map;

import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;

public class IntermediateParamValue extends Value {
  public final ParametricValue where;
  public final ParamValue toAdd;

  public IntermediateParamValue(ParametricValue where, ParamValue toAdd) {
    this.where = where;
    this.toAdd = toAdd;
  }

  @Override
  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("intermediateparam");
    tree.addChild(where.toLispTree());
    tree.addChild(toAdd.toLispTree());
    return tree;
  }

  @Override
  public Map<String, Object> toJson() {
    throw new RuntimeException("IntermediateParamValue should never be converted to JSON");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((toAdd == null) ? 0 : toAdd.hashCode());
    result = prime * result + ((where == null) ? 0 : where.hashCode());
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
    IntermediateParamValue other = (IntermediateParamValue) obj;
    if (toAdd == null) {
      if (other.toAdd != null)
        return false;
    } else if (!toAdd.equals(other.toAdd))
      return false;
    if (where == null) {
      if (other.where != null)
        return false;
    } else if (!where.equals(other.where))
      return false;
    return true;
  }

}
