package edu.stanford.nlp.sempre.thingtalk;

import java.util.Iterator;

import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;

public class ApplyFn extends SemanticFn {
  private boolean isAction;

  @Override
  public void init(LispTree tree) {
    super.init(tree);
    isAction = tree.children.size() > 1 && tree.child(1).value.equals("action");
  }

  @Override
  public DerivationStream call(final Example ex, final Callable c) {
    final Derivation left = c.child(0);
    if (left.value == null || !(left.value instanceof ParametricValue))
      throw new IllegalArgumentException("ApplyFn used incorrectly");

    final Iterator<Derivation> pseudoRight;
    // try all possible arguments to this channel
    ParametricValue pv = (ParametricValue) left.value;
    pseudoRight = pv.name.argtypes.entrySet().stream().map(e -> {
      String argname = e.getKey();
      String argtype = e.getValue();
      String argcanonical = pv.name.argcanonicals.get(e.getKey());

      // build a pseudo-derivation with the formula and the canonical
      Derivation.Builder bld = new Derivation.Builder()
          .canonicalUtterance(c.child(1).canonicalUtterance + " " + argcanonical)
          .value(new ParamNameValue(argname, argtype));
      return bld.createDerivation();
    }).iterator();

    return new MultipleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        while (true) {
        if (!pseudoRight.hasNext())
          return null;

          Derivation right = pseudoRight.next();

          ParametricValue leftValue = (ParametricValue) left.value;
          ParamNameValue rightValue = (ParamNameValue) right.value;

          if (isAction && leftValue.hasParamName(rightValue.argname))
            continue;

          // build a ParamValue with null operator and null value
          ParamValue pv = new ParamValue(rightValue, null, null, null);
          // wrap left and right into an intermediate value
          IntermediateParamValue ipv = new IntermediateParamValue(leftValue, pv);

          Derivation.Builder bld = new Derivation.Builder().withCallable(c).value(ipv)
              .canonicalUtterance(left.canonicalUtterance + " " + right.canonicalUtterance);
          return bld.createDerivation();
        }
      }
    };
  }

}
