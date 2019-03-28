#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/pareco-server/pareco-server-app.jar" "$@"
