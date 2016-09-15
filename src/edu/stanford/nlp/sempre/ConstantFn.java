package edu.stanford.nlp.sempre;

import fig.basic.LispTree;

/**
 * Just returns a fixed logical formula.
 *
 * @author Percy Liang
 */
public class ConstantFn extends SemanticFn {
  Formula formula;  // Formula to return

  public ConstantFn() { }

  public ConstantFn(Formula formula) {
    init(LispTree.proto.newList("ConstantFn", formula != null ? formula.toLispTree() : LispTree.proto.newLeaf("null")));
  }

  @Override
  public void init(LispTree tree) {
    super.init(tree);
    this.formula = Formulas.fromLispTree(tree.child(1));
  }

  @Override
  public DerivationStream call(final Example ex, final Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Derivation res = new Derivation.Builder()
                .withCallable(c)
                .formula(formula)
                .createDerivation();
        // don't generate feature if it is not grounded to a string
        if (FeatureExtractor.containsDomain("constant") && c.getStart() != -1)
          res.addFeature("constant", ex.phraseString(c.getStart(), c.getEnd()) + " --- " + formula.toString());
        return res;
      }
    };
  }
}
