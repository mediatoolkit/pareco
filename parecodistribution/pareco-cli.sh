#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/parecoclient/pareco-client-app.jar" "$@"
