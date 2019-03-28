#!/bin/bash

echo "Preparing build artifacts..."
VERSION=`cat ../pareco-core/target/classes/version.txt`

ARTIFACTS_SRC_DIR="target/pareco-distribution-$VERSION-bin"
ARTIFACTS_DST_DIR="target/pareco-distribution-bin"

echo "Version=$VERSION"

if [[ -d "$ARTIFACTS_DST_DIR" ]]; then
    echo "Removing $ARTIFACTS_DST_DIR"
    rm -r "$ARTIFACTS_DST_DIR"
fi

echo "Coping $ARTIFACTS_SRC_DIR -> $ARTIFACTS_DST_DIR"
cp -r "$ARTIFACTS_SRC_DIR" "$ARTIFACTS_DST_DIR"
