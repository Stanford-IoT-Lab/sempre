package edu.stanford.nlp.sempre.thingtalk;

import java.sql.SQLException;
import java.util.Iterator;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.Derivation.Cacheability;
import fig.basic.LispTree;
import fig.basic.Option;

/**
 * Uses the ThingpediaLexicon.
 *
 * Example: (rule $ROOT ($PHRASE) (ThingpediaLexiconFn "channel"))
 * 			for triggers, actions or queries
 *
 * 			(rule $ROOT ($PHRASE) (ThingpediaLexiconFn "app"))
 * 			for an application
 *
 * @author Giovanni Campagna
 */
public class ThingpediaLexiconFn extends SemanticFn {
  public static class Options {
    @Option(gloss = "Verbosity level")
    public int verbose = 0;
  }

  public static Options opts = new Options();

  private ThingpediaLexicon lexicon;

  private ThingpediaLexicon.Mode mode;
  private String filter;
  private boolean floating = false;

  public ThingpediaLexiconFn() {
  }

  @Override
  public void init(LispTree tree) {
    super.init(tree);

    String languageTag = tree.child(1).value;
    lexicon = ThingpediaLexicon.getForLanguage(languageTag);

    String mode = tree.child(2).value;
    // mode
    try {
      this.mode = ThingpediaLexicon.Mode.valueOf(mode.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid mode for ThingpediaLexiconFn", e);
    }

    if (tree.children.size() > 3)
      filter = tree.child(3).value;
    if (tree.children.size() > 4 && tree.child(4).value.equals("floating"))
      floating = true;
  }

  @Override
  public DerivationStream call(Example ex, Callable c) {
    String phrase;
    if (!floating)
      phrase = c.childStringValue(0);
    else
      phrase = null;
    Iterator<ThingpediaLexicon.Entry> entries;
    try {
      if (mode == ThingpediaLexicon.Mode.APP)
        entries = lexicon.lookupApp(phrase);
      else if (mode == ThingpediaLexicon.Mode.KIND)
        entries = lexicon.lookupKind(phrase);
      else if (mode == ThingpediaLexicon.Mode.PARAM)
        entries = lexicon.lookupParam(phrase);
      else
        entries = lexicon.lookupChannel(phrase, mode);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return new ThingpediaDerivationStream(ex, c, entries, phrase);
  }

  public class ThingpediaDerivationStream extends MultipleDerivationStream {
    private Example ex;
    private Callable callable;
    private Iterator<ThingpediaLexicon.Entry> entries;
    private String phrase;

    public ThingpediaDerivationStream(Example ex, Callable c, Iterator<ThingpediaLexicon.Entry> entries,
        String phrase) {
      this.ex = ex;
      this.callable = c;
      this.entries = entries;
      this.phrase = phrase;
    }

    @Override
    public Derivation createDerivation() {
      if (!entries.hasNext())
        return null;

      ThingpediaLexicon.Entry entry = entries.next();
      while (filter != null && !entry.applyFilter(filter)) {
        if (entries.hasNext()) {
          entry = entries.next();
        } else {
          entry = null;
          break;
        }
      }
      if (entry == null)
        return null;

      FeatureVector features = new FeatureVector();
      entry.addFeatures(features);
      Derivation deriv = new Derivation.Builder().withCallable(callable).formula(entry.toFormula())
          .localFeatureVector(features).canonicalUtterance(entry.getRawPhrase())
          .meetCache(Cacheability.LEXICON_DEPENDENT)
          .createDerivation();

      // Doesn't generalize, but add it for now, otherwise not separable
      if (FeatureExtractor.containsDomain("lexAlign"))
        deriv.addFeature("lexAlign", phrase + " --- " + entry.toFormula());

      if (SemanticFn.opts.trackLocalChoices)
        deriv.addLocalChoice("SimpleLexiconFn " + deriv.startEndString(ex.getTokens()) + " " + entry);

      return deriv;
    }
  }

}
