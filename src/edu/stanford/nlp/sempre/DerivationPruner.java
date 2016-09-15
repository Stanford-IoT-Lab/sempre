package edu.stanford.nlp.sempre;

import java.util.ArrayList;
import java.util.List;

import fig.basic.LispTree;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Prune derivations during parsing.
 *
 * To add custom pruning criteria, implement a DerivationPruningComputer class,
 * and put the class name in the |pruningComputers| option.
 *
 * @author ppasupat
 */

public class DerivationPruner {
  public static class Options {
    @Option public List<String> pruningStrategies = new ArrayList<>();
    @Option public List<String> pruningComputers = new ArrayList<>();
    @Option public int pruningVerbosity = 0;
    @Option public int maxNumValues = 10;
  }
  public static Options opts = new Options();

  public final Parser parser;
  public final Example ex;
  private List<DerivationPruningComputer> pruningComputers = new ArrayList<>();
  private List<String> customAllowedDomains;

  public DerivationPruner(ParserState parserState) {
    this.parser = parserState.parser;
    this.ex = parserState.ex;
    for (String pruningComputer : opts.pruningComputers) {
      try {
        Class<?> pruningComputerClass = Class.forName(SempreUtils.resolveClassName(pruningComputer));
        pruningComputers.add((DerivationPruningComputer) pruningComputerClass.getConstructor(this.getClass()).newInstance(this));
      } catch (ClassNotFoundException e1) {
        throw new RuntimeException("Illegal pruning computer: " + pruningComputer);
      } catch (Exception e) {
        e.printStackTrace();
        e.getCause().printStackTrace();
        throw new RuntimeException("Error while instantiating pruning computer: " + pruningComputer);
      }
    }
  }

  public void setCustomAllowedDomains(List<String> customAllowedDomains) {
    this.customAllowedDomains = customAllowedDomains;
  }

  protected boolean containsStrategy(String name) {
     return opts.pruningStrategies.contains(name) &&
         (customAllowedDomains == null || customAllowedDomains.contains(name));
  }

  public boolean isPruned(Derivation deriv) {
    if (opts.pruningStrategies.isEmpty() && pruningComputers.isEmpty()) return false;
    if (pruneFormula(deriv)) return true;
    if (pruneDenotation(deriv)) return true;
    for (DerivationPruningComputer pruningComputer : pruningComputers) {
      if (pruningComputer.isPruned(deriv)) return true;
    }
    return false;
  }

  // ============================================================
  // Formula-based Pruning
  // ============================================================

  private boolean pruneFormula(Derivation deriv) {
    return pruneSingleton(deriv) || pruneSuperlatives(deriv);
  }

  /**
   * Prune singleton formula at the root.
   */
  private boolean pruneSingleton(Derivation deriv) {
    if (!containsStrategy("singleton")) return false;
    return deriv.isRoot(ex.numTokens()) && deriv.formula instanceof ValueFormula;
  }

  /**
   * Prune strings of multiple superlatives.
   */
  private boolean pruneSuperlatives(Derivation deriv) {
    if (containsStrategy("multipleSuperlatives")) {
      // Prune if there are more than arg{max|min} appearing in the formula (don't need to be adjacent)
      List<LispTree> stack = new ArrayList<>();
      int count = 0;
      stack.add(deriv.formula.toLispTree());
      while (!stack.isEmpty()) {
        LispTree tree = stack.remove(stack.size() - 1);
        if (tree.isLeaf()) {
          if ("argmax".equals(tree.value) || "argmin".equals(tree.value)) {
            count++;
            if (count >= 2) {
              if (opts.pruningVerbosity >= 2)
                LogInfo.logs("PRUNED [multipleSuperlatives] %s", deriv.formula);
              return true;
            }
          }
        } else {
          for (LispTree subtree : tree.children)
            stack.add(subtree);
        }
      }
    }
    return false;
  }

  // ============================================================
  // Denotation-based Pruning
  // ============================================================

  /**
   * Pruning based on denotations.
   */
  private boolean pruneDenotation(Derivation deriv) {
    return pruneFinalDenotation(deriv);
  }

  private boolean pruneFinalDenotation(Derivation deriv) {
    Formula formula = deriv.formula;
    // Prune if the denotation is an empty list
    if (containsStrategy("emptyDenotation")) {
      deriv.ensureExecuted(parser.executor, ex.context);
      if (deriv.value instanceof ListValue) {
        if (((ListValue) deriv.value).values.isEmpty()) {
          if (opts.pruningVerbosity >= 3)
            LogInfo.logs("PRUNED [emptyDenotation] %s", formula);
          return true;
        }
      }
    }
    // Prune if the denotation is an error and the formula is not a partial formula
    if (containsStrategy("nonLambdaError") && !(deriv.formula instanceof LambdaFormula)) {
      deriv.ensureExecuted(parser.executor, ex.context);
      if (deriv.value instanceof ErrorValue) {
        if (opts.pruningVerbosity >= 3)
          LogInfo.logs("PRUNED [nonLambdaError] %s", formula);
        return true;
      }
    }
    // Prune if the denotation has too many values
    if (containsStrategy("tooManyValues") && deriv.isRoot(ex.numTokens())) {
      deriv.ensureExecuted(parser.executor, ex.context);
      if (deriv.value instanceof ListValue) {
        if (((ListValue) deriv.value).values.size() > opts.maxNumValues) {
          if (opts.pruningVerbosity >= 3)
            LogInfo.logs("PRUNED [tooManyValues] %s", formula);
          return true;
        }
      }
    }
    return false;
  }
}
