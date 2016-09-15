package edu.stanford.nlp.sempre;

import java.util.ArrayList;
import java.util.List;

import fig.basic.LispTree;
import fig.basic.Option;

/**
 * A semantic function takes a sequence of child derivations and produces a set
 * of parent derivations.  This is a pretty general concept, which can be used to:
 * - Generating candidates (lexicon)
 * - Do simple combination
 * - Filtering of derivations
 *
 * To override implement this function, you just need to fill out the call() function.
 *
 * @author Percy Liang
 */
public abstract class SemanticFn {
  public static class Options {
    @Option(gloss = "Whether or not to add to Derivation.localChoices during " +
        "function application (for debugging only).")
    public boolean trackLocalChoices = false;
  }

  public static final Options opts = new Options();

  // Used to define this SemanticFn.
  private LispTree tree;

  // Initialize the semantic function with any arguments (optional).
  // Override this function and call super.init(tree);
  public void init(LispTree tree) {
    this.tree = tree;
  }

  public interface Callable {
    String getCat();
    int getStart();
    int getEnd();
    Rule getRule();
    List<Derivation> getChildren();
    Derivation child(int i);
    String childStringValue(int i);
  }

  public static class CallInfo implements Callable {
    final String cat;
    final int start;
    final int end;
    final Rule rule;
    final List<Derivation> children;
    public CallInfo(String cat, int start, int end, Rule rule, List<Derivation> children) {
      this.cat = cat;
      this.start = start;
      this.end = end;
      this.rule = rule;
      this.children = children;
    }
    @Override
    public String getCat() { return cat; }
    @Override
    public int getStart() { return start; }
    @Override
    public int getEnd() { return end; }
    @Override
    public Rule getRule() { return rule; }
    @Override
    public List<Derivation> getChildren() { return children; }
    @Override
    public Derivation child(int i) { return children.get(i); }
    @Override
    public String childStringValue(int i) {
      return Values.getString(children.get(i).value);
    }

    public static final CallInfo NULL_INFO =
      new CallInfo("", -1, -1, Rule.nullRule, new ArrayList<Derivation>());
  }

  // Main entry point: return a stream of Derivations (possibly none).
  // The computation of the Derivations should be done lazily.
  public abstract DerivationStream call(Example ex, Callable c);

  public LispTree toLispTree() { return tree; }
  @Override public String toString() { return tree.toString(); }

 // default does nothing
  public void addFeedback(Example ex) { return; }

 // default does nothing
  public void sortOnFeedback(Params params) { return; }

}
