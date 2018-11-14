#!/bin/bash

DIR=$(dirname "$0")
java -jar "$DIR/parecoserver/pareco-server-app.jar" "$@"
