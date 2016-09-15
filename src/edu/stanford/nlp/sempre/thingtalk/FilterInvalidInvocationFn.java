package edu.stanford.nlp.sempre.thingtalk;

import java.util.Set;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.util.ArraySet;

public class FilterInvalidInvocationFn extends SemanticFn {
  private static boolean operatorOk(String operator, boolean isAction) {
    if (isAction)
      return operator.equals("is");
    else
      return true;
  }

  static boolean paramTypeOk(ParamNameValue param, ChannelNameValue channel) {
    return param.type.equals(channel.getArgType(param.argname));
  }

  private static boolean valueOk(Value value) {
    if (!(value instanceof ParametricValue))
      return true;

    boolean isAction = value instanceof ActionValue;

    ParametricValue pv = (ParametricValue) value;

    Set<String> names = new ArraySet<>();
    for (ParamValue param : pv.params) {
      if (!ArgFilterHelpers.valueOk(param))
        return false;
      if (!paramTypeOk(param.name, pv.name) || !operatorOk(param.operator, isAction))
        return false;
      if (isAction) {
        if (names.contains(param.name.argname))
          return false;
        else
          names.add(param.name.argname);
      }
    }

    return true;
  }

  @Override
  public DerivationStream call(Example ex, Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Derivation child = c.child(0);

        // we should generate valid stuff by construction, so complain loudly if that's
        // not the case
        if (child.getValue() != null && !valueOk(child.getValue()))
          throw new RuntimeException("Generated an invalid derivation");

        return new Derivation.Builder()
            .withCallable(c)
            .withValueFrom(child)
            .createDerivation();
      }
    };
  }

}
