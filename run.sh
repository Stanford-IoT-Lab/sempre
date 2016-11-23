#!/bin/sh

set -x

usage() {
	echo "run.sh: [interactive|training|server] [extra SEMPRE args...]"
}

MODE=$1
shift

EXTRA_ARGS="$@"

JAVA=${JAVA:-java}
BASE_ARGS="-ea $JAVA_ARGS -Dmodules=core,corenlp,overnight,thingtalk,api -cp libsempre/*:lib/*"

case $MODE in
	interactive)
		TARGET=edu.stanford.nlp.sempre.Main
		MODE_ARGS="++sabrina/sabrina.interactive.conf"
		;;
	training)
		TARGET=edu.stanford.nlp.sempre.Main
		MODE_ARGS="++sabrina/sabrina.training.conf"
		;;
	server)
		TARGET=edu.stanford.nlp.sempre.api.APIServer
		BASE_ARGS="${BASE_ARGS} -Djava.library.path=jni"
		MODE_ARGS="++sabrina/sabrina.server.conf"
		;;
	interactive)
		usage
		exit 1
		;;
esac

exec ${JAVA} ${BASE_ARGS} ${TARGET} ${MODE_ARGS} ${EXTRA_ARGS}
