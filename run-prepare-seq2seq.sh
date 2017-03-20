#!/bin/sh

set -x

LANGUAGE_TAG=${LANGUAGE_TAG:-en}
SEMPREDIR=`dirname $0`

EXTRA_ARGS="$@"

JAVA=${JAVA:-java}
BASE_ARGS="-ea $JAVA_ARGS -Dmodules=core,corenlp,overnight,thingtalk,api -cp ${SEMPREDIR}/libsempre/*:${SEMPREDIR}/lib/*"

exec ${JAVA} ${BASE_ARGS} edu.stanford.nlp.sempre.thingtalk.seq2seq.PrepareSeq2Seq \
	++${SEMPREDIR}/sabrina/sabrina.seq2seq.conf \
	-PrepareSeq2Seq.languageTag ${LANGUAGE_TAG} \
	-CoreNLPAnalyzer.languageTag ${LANGUAGE_TAG} \
	-ThingpediaDataset.languageTag ${LANGUAGE_TAG} \
	${EXTRA_ARGS}
