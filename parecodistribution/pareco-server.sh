#!/bin/bash

DIR=$(dirname "$0")
exec java -jar "$DIR/parecoserver/pareco-server-app.jar" "$@"
