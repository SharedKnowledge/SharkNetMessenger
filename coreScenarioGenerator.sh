#!/bin/bash
set -e

# clean + build
rm -rf bin
mkdir bin
javac -d bin $(find src/ASAPEngineTestSuite -name "*.java")

echo "Manifest-Version: 1.0" > manifest.mf
echo "Main-Class: asapEngineTestSuite.CoreScenarioOutput" >> manifest.mf

# create executable jar
jar cfm scriptgenerator.jar manifest.mf -C bin .

# cleanup
rm manifest.mf