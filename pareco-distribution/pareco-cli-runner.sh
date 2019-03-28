#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/pareco-client-runner/pareco-client-runner-app.jar" "$@"
