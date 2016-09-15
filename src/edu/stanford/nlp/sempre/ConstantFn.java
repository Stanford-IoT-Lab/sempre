package edu.stanford.nlp.sempre;

import fig.basic.LispTree;

/**
 * Just returns a fixed logical formula.
 *
 * @author Percy Liang
 */
public class ConstantFn extends SemanticFn {
  Value value;

  public ConstantFn() { }

  public ConstantFn(Value value) {
    init(LispTree.proto.newList("ConstantFn", value != null ? value.toLispTree() : LispTree.proto.newLeaf("null")));
  }

  @Override
  public void init(LispTree tree) {
    super.init(tree);
    this.value = Values.fromLispTree(tree.child(1));
  }

  @Override
  public DerivationStream call(final Example ex, final Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Derivation res = new Derivation.Builder()
            .withCallable(c)
            .value(value)
            .createDerivation();
        // don't generate feature if it is not grounded to a string
        if (FeatureExtractor.containsDomain("constant") && c.getStart() != -1)
          res.addFeature("constant", ex.phraseString(c.getStart(), c.getEnd()) + " --- " + value.toString());
        return res;
      }
    };
  }
}
