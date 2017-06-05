#!/bin/sh

set -e
set -x

MODULE=${MODULE:-sabrina}
CANONICALS=${1:-./${MODULE}/${MODULE}.canonicals.tsv}
SEMPREDIR=$(realpath `dirname $0`)
BERKELEYALIGNER=${BERKELEYALIGNER:-${SEMPREDIR}/../berkeleyaligner}

# prepare the berkeley aligner input
rm -fr ./berkeleyaligner.tmp
mkdir -p ./berkeleyaligner.tmp/${MODULE}/test
mkdir -p ./berkeleyaligner.tmp/${MODULE}/train
java -cp "${SEMPREDIR}/libsempre/*:${SEMPREDIR}/lib/*" \
	-Dmodules=core,corenlp,overnight,thingtalk \
	edu.stanford.nlp.sempre.overnight.Tokenizer \
	${CANONICALS} \
	./berkeleyaligner.tmp/${MODULE}/train/train.f \
	./berkeleyaligner.tmp/${MODULE}/train/train.e \
	${LANGUAGE_TAG}

# run the berkeley aligner
test -f ${MODULE}/berkeleydictionary.${LANGUAGE_TAG} && cp ${MODULE}/berkeleydictionary.${LANGUAGE_TAG} berkeleyaligner.tmp/${MODULE}/dictionary
( cd ./berkeleyaligner.tmp ; java -ea -jar ${BERKELEYALIGNER}/berkeleyaligner.jar ++${SEMPREDIR}/${MODULE}/${MODULE}.berkeleyaligner.conf )
paste ./berkeleyaligner.tmp/output/training.f \
      ./berkeleyaligner.tmp/output/training.e \
      ./berkeleyaligner.tmp/output/training.align > ./${MODULE}/${MODULE}.word_alignments.berkeley.source.${LANGUAGE_TAG}

# convert the berkeley aligner format to something sempre likes
java -cp "${SEMPREDIR}/libsempre/*:${SEMPREDIR}/lib/*" \
	-Dmodules=core,corenlp,overnight,thingtalk \
	edu.stanford.nlp.sempre.overnight.Aligner \
	./${MODULE}/${MODULE}.word_alignments.berkeley.source.${LANGUAGE_TAG} \
	./${MODULE}/${MODULE}.word_alignments.berkeley.${LANGUAGE_TAG} berkeley 2
rm -fr ./berkeleyaligner.tmp

# run the phrase aligner
java -cp "${SEMPREDIR}/libsempre/*:${SEMPREDIR}/lib/*" \
	-Dmodules=core,corenlp,overnight,thingtalk \
	edu.stanford.nlp.sempre.overnight.PhraseAligner \
	./${MODULE}/${MODULE}.word_alignments.berkeley.source.${LANGUAGE_TAG} \
	./${MODULE}/${MODULE}.phrase_alignments.${LANGUAGE_TAG} \
	${LANGUAGE_TAG} 2
