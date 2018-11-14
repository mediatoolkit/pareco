#!/bin/bash

DIR=$(dirname "$0")
java -jar "$DIR/parecoclient/pareco-client-app.jar" "$@"
