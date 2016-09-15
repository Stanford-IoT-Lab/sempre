package edu.stanford.nlp.sempre.test;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import edu.stanford.nlp.sempre.Formula;
import edu.stanford.nlp.sempre.Formulas;

/**
 * Test Formulas.
 * @author Percy Liang
 */
public class FormulaTest {
  private static Formula F(String s) { return Formula.fromString(s); }

  @Test
  public void simpleFormula() {
    // Bound, shouldn't replace x
    assertEquals(F("((lambda x (f (var x))) (var y))"),
                 Formulas.substituteVar(F("((lambda x (f (var x))) (var y))"), "x", F("a")));

    // Free, should replace y
    assertEquals(F("((lambda x (f (var x))) a)"),
                 Formulas.substituteVar(F("((lambda x (f (var x))) (var y))"), "y", F("a")));
  }
}
