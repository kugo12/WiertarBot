#!/usr/bin/env sh

set -eux

mkdir data | true
cp -r static-data/* data/
java -jar ./app.jar
