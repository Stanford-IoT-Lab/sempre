package edu.stanford.nlp.sempre;

import com.google.common.base.Function;

import fig.basic.LispTree;

/**
 * Utilities for working with Formulas.
 *
 * @author Percy Liang
 */
public abstract class Formulas {
  public static Formula fromLispTree(LispTree tree) {
    // Try to interpret as ValueFormula
    if (tree.isLeaf())  // Leaves are name values
      return new ValueFormula<>(new NameValue(tree.value, null));
    Value value = Values.fromLispTreeOrNull(tree);  // General case
    if (value != null)
      return new ValueFormula<>(value);

    throw new RuntimeException("Unsupported formula syntax");
  }

  // Replace occurrences of the variable reference |var| with |formula|.
  public static Formula substituteVar(Formula formula, final String var, final Formula replaceFormula) {
    return formula.map(
        new Function<Formula, Formula>() {
          @Override
          public Formula apply(Formula formula) {
            return null;
          }
        });
  }

  // Replace top-level occurrences of |searchFormula| inside |formula| with |replaceFormula|.
  public static Formula substituteFormula(Formula formula, final Formula searchFormula, final Formula replaceFormula) {
    return formula.map(
        new Function<Formula, Formula>() {
          @Override
          public Formula apply(Formula formula) {
            if (formula.equals(searchFormula)) return replaceFormula;
            return null;
          }
        });
  }

  public static String getString(Formula formula) {
    if (formula instanceof ValueFormula) {
      Value value = ((ValueFormula) formula).value;
      if (value instanceof StringValue)
        return ((StringValue) value).value;
      if (value instanceof NameValue)
        return ((NameValue) value).id;
      if (value instanceof NumberValue)
        return ((NumberValue) value).value + "";
    }
    return null;
  }

  public static String getNameId(Formula formula) {
    if (formula instanceof ValueFormula) {
      Value value = ((ValueFormula) formula).value;
      if (value instanceof NameValue)
        return ((NameValue) value).id;
    }
    return null;
  }

  public static double getDouble(Formula formula) {
    if (formula instanceof ValueFormula) {
      Value value = ((ValueFormula) formula).value;
      if (value instanceof NumberValue)
        return ((NumberValue) value).value;
    }
    return Double.NaN;
  }

  public static int getInt(Formula formula) {
    return (int) getDouble(formula);
  }

  public static ValueFormula<NameValue> newNameFormula(String id) {
    return new ValueFormula<>(new NameValue(id));
  }
}
