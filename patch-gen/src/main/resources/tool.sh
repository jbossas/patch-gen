#!/bin/bash

PROGNAME="$(readlink -f ${BASH_SOURCE[0]})"
DIRNAME="$(dirname $PROGNAME)"

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

eval \"$JAVA\" -jar \"$DIRNAME/lib/tool.jar\" "$@"
