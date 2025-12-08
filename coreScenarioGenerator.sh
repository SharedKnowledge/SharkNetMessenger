#!/bin/bash
set -e

# output directory for the generated jar
OUT_DIR=basic_testing

# clean + build
rm -rf bin
mkdir -p bin

# compile sources (use lowercase path matching the repo structure)
SRC_DIR=src/asapEngineTestSuite
javac -d bin $(find "$SRC_DIR" -name "*.java")

# ensure output directory exists
mkdir -p "$OUT_DIR"

echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: asapEngineTestSuite.CoreScenarioOutput" >> manifest.mf

# create executable jar inside the alpha_testing directory
jar cfm "$OUT_DIR/scriptgenerator.jar" manifest.mf -C bin .

# cleanup
rm manifest.mf