package edu.stanford.nlp.sempre.thingtalk;

import edu.stanford.nlp.sempre.*;

public class AddValueFn extends SemanticFn {
  @Override
  public DerivationStream call(final Example ex, final Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Derivation left = c.child(0);
        Derivation right = c.child(1);

        IntermediateParamValue ipv = (IntermediateParamValue) left.value;
        ParamNameValue param = ipv.toAdd.name;
        
        String haveType = ThingTalk.typeFromValue(right.value);
        if (!ArgFilterHelpers.typeOk(haveType, param.type, right.value) &&
            !ArgFilterHelpers.typeOkArray(haveType, param.type, right.value))
          return null;

        // make the final ParamValue
        ParamValue pv2 = new ParamValue(ipv.toAdd.name, haveType, ipv.toAdd.operator, right.value);

        // add it to the action/query/trigger
        ParametricValue newInvocation = ipv.where.clone();
        newInvocation.add(pv2);
        
        return new Derivation.Builder().withCallable(c).value(newInvocation)
            .createDerivation();
      }
    };
  }
}
