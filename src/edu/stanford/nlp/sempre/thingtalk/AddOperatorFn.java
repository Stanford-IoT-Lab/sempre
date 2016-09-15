package edu.stanford.nlp.sempre.thingtalk;

import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;

public class AddOperatorFn extends SemanticFn {
  private boolean isAction;

  @Override
  public void init(LispTree tree) {
    super.init(tree);
    isAction = tree.children.size() > 1 && tree.child(1).value.equals("action");
  }

  private static boolean operatorOk(String paramType, String operator) {
    switch (operator) {
    case "is":
      return !paramType.startsWith("Array(");
    case "contains":
      return paramType.equals("String");
    case "has":
      return paramType.startsWith("Array(");
    case ">":
    case "<":
      return paramType.equals("Number") || paramType.startsWith("Measure(");
    default:
      return true;
    }
  }

  @Override
  public DerivationStream call(final Example ex, final Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Derivation left = c.child(0);
        Derivation right = c.child(1);
        
        IntermediateParamValue ipv = (IntermediateParamValue) left.value;
        ParamNameValue param = ipv.toAdd.name;
        StringValue operator = (StringValue) right.value;
        
        if (isAction) {
          if (!operator.value.equals("is"))
            return null;
        } else {
          if (!operatorOk(param.type, operator.value))
            return null;
        }
        
        // make a new IntermediateParamValue with the operator set
        IntermediateParamValue ipv2 = new IntermediateParamValue(ipv.where, new ParamValue(ipv.toAdd.name, null, operator.value, null));

        return new Derivation.Builder().withCallable(c).formula(new ValueFormula<>(ipv2))
            .createDerivation();
      }
    };
  }
}
