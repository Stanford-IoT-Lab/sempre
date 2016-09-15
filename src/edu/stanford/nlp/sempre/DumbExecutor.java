package edu.stanford.nlp.sempre;

public class DumbExecutor extends Executor {

  @Override
  public Response execute(Formula formula, ContextValue context) {
    return new Response(((ValueFormula<?>) formula).value);
  }

}
