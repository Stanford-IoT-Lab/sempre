package edu.stanford.nlp.sempre.thingtalk;

import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;

public class CommandFn extends SemanticFn {
  private String commandType;
  private String secondArg;
  private boolean withDevice;

  @Override
  public void init(LispTree tree) {
    super.init(tree);
    commandType = tree.child(1).value;
    if (tree.children.size() > 2) {
      secondArg = tree.child(2).value;
      withDevice = tree.child(2).value.equals("withDevice");
    } else {
      secondArg = "generic";
      withDevice = false;
    }
  }

  @Override
  public DerivationStream call(Example ex, Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        NameValue device;
        Value cmd;
        if (withDevice) {
          device = (NameValue) c.child(0).value;
          cmd = ThingTalk.cmdForm(new StringValue(commandType), device);
        } else {
          cmd = ThingTalk.cmdForm(new StringValue(commandType), new StringValue(secondArg));
        }

        return new Derivation.Builder().withCallable(c).value(cmd).createDerivation();
      }
    };
  }
}
