package edu.stanford.nlp.sempre;

import java.util.ArrayList;
import java.util.List;

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
    for (DerivationPruningComputer pruningComputer : pruningComputers) {
      if (pruningComputer.isPruned(deriv)) return true;
    }
    return false;
  }
}
