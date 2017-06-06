#!/bin/sh

set -e
set -x

LANGUAGE_TAG=${LANGUAGE_TAG:-en}
WORKDIR=`pwd`/workdir
export WORKDIR
SEMPREDIR=$(realpath $(dirname $0))
export SEMPREDIR
MODULE=${MODULE:-sabrina}

rm -fr ${WORKDIR}/sempre.tmp
test ${SEMPREDIR} != "." && cp ${SEMPREDIR}/module-classes.txt ${WORKDIR}

cat > ${WORKDIR}/${MODULE}/${MODULE}.training.conf <<EOF
!include ${SEMPREDIR}/${MODULE}/${MODULE}.conf
execDir ${WORKDIR}/sempre.tmp

# CoreNLP
CoreNLPAnalyzer.languageTag ${LANGUAGE_TAG}

# Grammar
Grammar.inPaths ${SEMPREDIR}/${MODULE}/${MODULE}.${LANGUAGE_TAG}.grammar

# Dataset
#Builder.dataset thingtalk.ThingpediaDataset
ThingpediaDataset.languageTag ${LANGUAGE_TAG}
Dataset.devFrac 0.1
Dataset.trainFrac 0.9
Dataset.splitDevFromTrain false
# note that we don't set splitDevFromTrain here
# run-manual-training-pipeline.sh would do that, if needed

# Features
FeatureExtractor.languageTag ${LANGUAGE_TAG}
OvernightFeatureComputer.wordAlignmentPath ${WORKDIR}/${MODULE}/${MODULE}.word_alignments.berkeley
OvernightFeatureComputer.phraseAlignmentPath ${WORKDIR}/${MODULE}/${MODULE}.phrase_alignments
PPDBModel.ppdbModelPath ${SEMPREDIR}/sabrina/sabrina-ppdb.txt

# Training
Learner.maxTrainIters 2
Learner.numThreads 8
Learner.batchSize 75
Params.l1Reg nonlazy
Params.l1RegCoeff 0.0001
EOF

# update the lexicon
#/opt/sempre/run-lexicon-builder.sh -ThingpediaDatabase.dbPw ${DATABASE_PW}
#rm -fr ${WORKDIR}/sempre.tmp

# run sempre
${SEMPREDIR}/run.sh training "$@"

# move the gxenerated file where APIServer will know to look for
cp ${WORKDIR}/sempre.tmp/params.2 ${WORKDIR}/${MODULE}/${MODULE}.${LANGUAGE_TAG}.params
#rm -fr ${WORKDIR}/sempre.tmp
