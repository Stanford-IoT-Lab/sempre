package edu.stanford.nlp.sempre;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

import fig.basic.Option;
import fig.basic.Pair;
import fig.basic.Utils;

/**
 * Contains all the components (grammar, feature extractor, parser, parameters)
 * needed for semantic parsing.
 *
 * @author Percy Liang
 */
public class Builder {
  public static class Options {
    @Option public String inParamsPath;
    @Option public String executor = "JavaExecutor";
    @Option public String valueEvaluator = "ExactValueEvaluator";
    @Option public String parser = "BeamParser";
    @Option
    public String dataset = "Dataset";
    @Option
    public List<Pair<String, String>> languageInParamsPath = new ArrayList<>();
  }

  public static Options opts = new Options();

  public Grammar grammar;
  public Executor executor;
  public ValueEvaluator valueEvaluator;
  public FeatureExtractor extractor;
  public Parser parser;
  public Params params;
  public AbstractDataset dataset;

  public void build() {
    grammar = null;
    executor = null;
    valueEvaluator = null;
    extractor = null;
    parser = null;
    params = null;
    buildUnspecified();
  }

  public void buildForLanguage(String languageTag) {
    grammar = null;
    executor = null;
    valueEvaluator = null;
    extractor = null;
    parser = null;
    params = null;

    if (grammar == null) {
      grammar = new Grammar();
      grammar.readForLanguage(languageTag);
      // don't write out the grammar for a non-default language
    }

    // Parameters
    if (params == null) {
      params = new Params();

      String precomputed = null;
      for (Pair<String, String> pair : opts.languageInParamsPath) {
        if (pair.getFirst().equals(languageTag)) {
          precomputed = pair.getSecond();
          break;
        }
      }
      if (!Strings.isNullOrEmpty(precomputed) && (new File(precomputed)).exists())
        params.read(precomputed);
    }

    // Executor
    if (executor == null)
      executor = (Executor) Utils.newInstanceHard(SempreUtils.resolveClassName(opts.executor));

    // Feature extractor
    if (extractor == null)
      extractor = new FeatureExtractor(executor, languageTag);

    buildLanguageAgnostic();
  }

  public void buildUnspecified() {
    // Grammar
    if (grammar == null) {
      grammar = new Grammar();
      grammar.read();
      grammar.write();
    }

    // Parameters
    if (params == null) {
      params = new Params();
      if (!Strings.isNullOrEmpty(opts.inParamsPath))
        params.read(opts.inParamsPath);
    }

    // Executor
    if (executor == null)
      executor = (Executor) Utils.newInstanceHard(SempreUtils.resolveClassName(opts.executor));

    // Feature extractor
    if (extractor == null)
      extractor = new FeatureExtractor(executor);

    buildLanguageAgnostic();
  }

  private void buildLanguageAgnostic() {
    // Value evaluator
    if (valueEvaluator == null)
      valueEvaluator = (ValueEvaluator) Utils.newInstanceHard(SempreUtils.resolveClassName(opts.valueEvaluator));

    // Parser
    if (parser == null)
      parser = buildParser(new Parser.Spec(grammar, extractor, executor, valueEvaluator));

    // Dataset
    if (dataset == null) {
      dataset = (AbstractDataset) Utils.newInstanceHard(SempreUtils.resolveClassName(opts.dataset));
      try {
        dataset.read();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static Parser buildParser(Parser.Spec spec) {
    switch (opts.parser) {
    case "BeamParser":
      return new BeamParser(spec);
    case "ReinforcementParser":
      return new ReinforcementParser(spec);
    case "FloatingParser":
      return new FloatingParser(spec);
    default:
      // Try instantiating by name
      try {
        Class<?> parserClass = Class.forName(SempreUtils.resolveClassName(opts.parser));
        return (Parser) parserClass.getConstructor(spec.getClass()).newInstance(spec);
      } catch (ClassNotFoundException e1) {
        throw new RuntimeException("Illegal parser: " + opts.parser);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Error while instantiating parser: " + opts.parser + "\n" + e);
      }
    }
  }
}
