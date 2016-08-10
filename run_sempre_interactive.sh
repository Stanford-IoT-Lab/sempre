#!/bin/sh

# The overnight paper does not include "rule" features
# I have them because they help the parser ignore too many
# derivation that use $StringValue (which is a catch all)
exec java -ea '-Dmodules=core,overnight,thingtalk' \
              '-Djava.library.path=jni' \
              '-cp' 'libsempre/*:lib/*' \
              'edu.stanford.nlp.sempre.Main' \
              '-LanguageAnalyzer' 'corenlp.CoreNLPAnalyzer' \
              '-Builder.parser' 'BeamParser' \
              '-Builder.executor' 'JavaExecutor' \
              '-JavaExecutor.unpackValues' 'false' \
              '-Grammar.inPaths' 'thingtalk/thingtalk.grammar' \
              '-Grammar.tags' 'parse' '+Grammar.tags' 'general' \
              '-FeatureExtractor.featureDomains' 'denotation' 'rule' \
              '-FeatureExtractor.featureComputers' 'overnight.OvernightFeatureComputer' \
              '-OvernightFeatureComputer.featureDomains' \
              'match' 'ppdb' 'skip-bigram' 'root' 'alignment' 'lexical' \
              'root_lexical' \
              '-FloatingParser.maxDepth' '12' \
              '-Parser.beamSize' '9' \
              '-wordAlignmentPath' 'thingtalk/thingtalk.word_alignments.berkeley' \
              '-phraseAlignmentPath' 'thingtalk/thingtalk.phrase_alignments' \
              '-PPDBModel.ppdbModelPath' 'thingtalk/thingtalk-ppdb.txt' \
              '-Learner.maxTrainIters' '1' \
              '-SimpleLexicon.inPaths' 'thingtalk/thingtalk.lexicon' \
              '-DataSet.inPaths' 'train:thingtalk/thingtalk.examples' \
              '-Main.interactive' 'true' \
              '-ThingpediaDatabase.dbUrl' 'jdbc:mysql://thingengine.crqccvnuyu19.us-west-2.rds.amazonaws.com/thingengine' \
              '-ThingpediaDatabase.dbUser' 'sempre' \
              '-BeamParser.executeAllDerivations' 'true' \
              '-FloatingParser.executeAllDerivations' 'true' \
              "$@"
