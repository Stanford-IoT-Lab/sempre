#!/bin/sh

BERKELEY_ALIGNER=${BERKELEY_ALIGNER:-`pwd`/../berkeleyaligner}
CANONICALS=${1:-./sabrina/sabrina.canonicals.tsv}

# run the berkeley aligner
rm -fr ./berkeleyaligner.tmp
mkdir -p ./berkeleyaligner.tmp/sabrina/test
mkdir -p ./berkeleyaligner.tmp/sabrina/train
cut -f1 $CANONICALS > ./berkeleyaligner.tmp/sabrina/train/train.f
cut -f2 $CANONICALS > ./berkeleyaligner.tmp/sabrina/train/train.e
( cd ./berkeleyaligner.tmp ; java -ea -jar $BERKELEY_ALIGNER/berkeleyaligner.jar ++../sabrina/sabrina.berkeleyaligner.conf )
paste ./berkeleyaligner.tmp/output/training.f ./berkeleyaligner.tmp/output/training.e ./berkeleyaligner.tmp/output/training.align > ./sabrina/sabrina.word_alignments.berkeley.source

# convert the berkeley aligner format to something sempre likes
java -cp 'libsempre/*:lib/*' -Dmodules=core,corenlp,overnight,thingtalk edu.stanford.nlp.sempre.overnight.Aligner ./sabrina/sabrina.word_alignments.berkeley.source ./sabrina/sabrina.word_alignments.berkeley berkeley 2
rm -fr ./berkeleyaligner.tmp
