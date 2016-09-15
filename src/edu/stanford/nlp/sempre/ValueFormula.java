package edu.stanford.nlp.sempre;

import java.util.List;

import com.google.common.base.Function;

import fig.basic.LispTree;

/**
 * A ValueFormula represents an atomic value which is cannot be decomposed
 * into further symbols.  Simply a wrapper around Value.
 *
 * @author Percy Liang
 */
public class ValueFormula<T extends Value> extends Formula {
  public final T value;

  public ValueFormula(T value) { this.value = value; }
  @Override
  public LispTree toLispTree() {
    if (value instanceof NameValue) return LispTree.proto.newLeaf(((NameValue) value).id);
    return value.toLispTree();
  }

  @Override
  public Formula map(Function<Formula, Formula> func) {
    Formula result = func.apply(this);
    return result == null ? this : result;
  }

  @Override
  public List<Formula> mapToList(Function<Formula, List<Formula>> func, boolean alwaysRecurse) {
    return func.apply(this);
  }

  @SuppressWarnings({"equalshashcode"})
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValueFormula<?> that = (ValueFormula<?>) o;
    if (!value.equals(that.value)) return false;
    return true;
  }

  @Override
  public int computeHashCode() {
    return value.hashCode();
  }
}
