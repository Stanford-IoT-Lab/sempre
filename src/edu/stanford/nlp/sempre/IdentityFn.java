package edu.stanford.nlp.sempre;

/**
 * Identity function.
 *
 * @author Percy Liang
 */
public class IdentityFn extends SemanticFn {
  @Override
  public DerivationStream call(Example ex, final Callable c) {
    return new SingleDerivationStream() {
      @Override
      public Derivation createDerivation() {
        return new Derivation.Builder()
            .withCallable(c)
            .withValueFrom(c.child(0))
            .createDerivation();
      }
    };
  }
}
