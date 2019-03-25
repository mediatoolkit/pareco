#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/parecoclientrunner/pareco-client-runner-app.jar" "$@"
