package edu.stanford.nlp.sempre;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

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

    String func = tree.child(0).value;
    if (func != null) {
      if (func.equals("var"))
        return new VariableFormula(tree.child(1).value);
      if (func.equals("lambda"))
        return new LambdaFormula(tree.child(1).value, fromLispTree(tree.child(2)));
      if (func.equals("call")) {
        Formula callFunc = fromLispTree(tree.child(1));
        List<Formula> args = Lists.newArrayList();
        for (int i = 2; i < tree.children.size(); i++)
          args.add(fromLispTree(tree.child(i)));
        return new CallFormula(callFunc, args);
      }
    }

    // Default is join: (fb:type.object.type fb:people.person)
    if (tree.children.size() != 2)
      throw new RuntimeException("Invalid number of arguments for join (want 2): " + tree);
    return new JoinFormula(fromLispTree(tree.child(0)), fromLispTree(tree.child(1)));
  }

  // Special case to enable "argmax 1 1" rather than "argmax (number 1) (number 1)"
  private static Formula parseIntToFormula(LispTree tree) {
    try {
      int i = Integer.parseInt(tree.value);
      double d = i;
      NumberValue value = new NumberValue(d);
      return new ValueFormula(value);
    } catch (NumberFormatException e) {
      Formula formula = fromLispTree(tree);
      if (!(formula instanceof PrimitiveFormula))
        throw new RuntimeException("Rank and count of argmax must be variables or numbers");
      return formula;
    }
  }

  // Replace occurrences of the variable reference |var| with |formula|.
  public static Formula substituteVar(Formula formula, final String var, final Formula replaceFormula) {
    return formula.map(
        new Function<Formula, Formula>() {
          @Override
          public Formula apply(Formula formula) {
            if (formula instanceof VariableFormula) {  // Replace variable
              String name = ((VariableFormula) formula).name;
              return var.equals(name) ? replaceFormula : formula;
            } else if (formula instanceof LambdaFormula) {
              if (((LambdaFormula) formula).var.equals(var)) // |var| is bound, so don't substitute inside
                return formula;
            }
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

  // Beta-reduction.
  public static Formula lambdaApply(LambdaFormula func, Formula arg) {
    return substituteVar(func.body, func.var, arg);
  }

  // Apply all the nested LambdaFormula's.
  public static Formula betaReduction(Formula formula) {
    return formula.map(
        new Function<Formula, Formula>() {
          @Override
          public Formula apply(Formula formula) {
            if (formula instanceof JoinFormula) {
              Formula relation = betaReduction(((JoinFormula) formula).relation);
              Formula child = ((JoinFormula) formula).child;
              if (relation instanceof LambdaFormula)
                return betaReduction(lambdaApply((LambdaFormula) relation, child));
            }
            return null;
          }
        });
  }

  // Return whether |formula| contains a free instance of |var|.
  public static boolean containsFreeVar(Formula formula, VariableFormula var) {
    if (formula instanceof PrimitiveFormula)
      return formula.equals(var);
    if (formula instanceof JoinFormula) {
      JoinFormula join = (JoinFormula) formula;
      return containsFreeVar(join.relation, var) || containsFreeVar(join.child, var);
    }
    if (formula instanceof LambdaFormula) {
      LambdaFormula lambda = (LambdaFormula) formula;
      if (lambda.var.equals(var.name)) return false;  // Blocked by bound variable
      return containsFreeVar(lambda.body, var);
    }
    throw new RuntimeException("Unhandled: " + formula);
  }

  // TODO(joberant): use Formula.map, and use CanonicalNames.isReverseProperty, etc.
  public static Set<String> extractAtomicFreebaseElements(Formula formula) {
    Set<String> res = new HashSet<>();
    LispTree formulaTree = formula.toLispTree();
    extractAtomicFreebaseElements(formulaTree, res);
    return res;
  }
  private static void extractAtomicFreebaseElements(LispTree formulaTree,
                                                    Set<String> res) {
    if (formulaTree.isLeaf()) {  // base
      if (formulaTree.value.startsWith("fb:"))
        res.add(formulaTree.value);
      else if (formulaTree.value.startsWith("!fb:"))
        res.add(formulaTree.value.substring(1));
    } else {  // recursion
      for (LispTree child : formulaTree.children) {
        extractAtomicFreebaseElements(child, res);
      }
    }
  }

  // TODO(jonathan): move to feature extractor (this function doesn't seem fundamental)
  public static boolean isCountFormula(Formula formula) {
    if (formula instanceof JoinFormula) {
      Formula relation = ((JoinFormula) formula).relation;
      if (relation instanceof LambdaFormula) {
        Formula l = ((LambdaFormula) relation).body;
      }
    }
    return false;
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
    } else if (formula instanceof VariableFormula) {
      return ((VariableFormula) formula).name;
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

  /*
   * Extract all subformulas in a string format (to also have primitive values)
   * TODO(joberant): replace this with Formulas.map
   */
  public static Set<String> extractSubparts(Formula f) {
    Set<String> res = new HashSet<>();
    extractSubpartsRecursive(f, res);
    return res;
  }

  private static void extractSubpartsRecursive(Formula f, Set<String> res) {
    // base
    res.add(f.toString());
    // recurse
    if (f instanceof CallFormula) {
      CallFormula callFormula = (CallFormula) f;
      extractSubpartsRecursive(callFormula.func, res);
      for (Formula argFormula : callFormula.args)
        extractSubpartsRecursive(argFormula, res);
    } else if (f instanceof JoinFormula) {
      JoinFormula joinFormula = (JoinFormula) f;
      extractSubpartsRecursive(joinFormula.relation, res);
      extractSubpartsRecursive(joinFormula.child, res);
    } else if (f instanceof LambdaFormula) {
      LambdaFormula lambdaFormula = (LambdaFormula) f;
      extractSubpartsRecursive(lambdaFormula.body, res);
    }
  }
}
