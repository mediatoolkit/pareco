#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/pareco-client/pareco-client-app.jar" "$@"
