package edu.stanford.nlp.sempre.thingtalk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;

public class CallFn extends SemanticFn {
  private int arity;
  private Method method;

  @Override
  public void init(LispTree tree) {
    super.init(tree);

    String function = tree.child(1).value;
    arity = Integer.parseInt(tree.child(2).value);

    try {
      int idx = function.lastIndexOf('.');
      String className = function.substring(0, idx);
      String methodName = function.substring(idx + 1);

      Class<?> _class = Class.forName(className);

      Class<?>[] parameters = new Class<?>[arity];
      for (int i = 0; i < arity; i++)
        parameters[i] = Value.class;
      method = _class.getMethod(methodName, parameters);
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DerivationStream call(Example ex, Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        Object o;
        try {
          if (arity == 1)
            o = method.invoke(null, c.child(0).value);
          else if (arity == 2)
            o = method.invoke(null, c.child(0).value, c.child(1).value);
          else
            throw new RuntimeException();
        } catch (InvocationTargetException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }

        return new Derivation.Builder()
            .withCallable(c)
            .formula(new ValueFormula<>((Value) o))
            .createDerivation();
      }
    };
  }

}
