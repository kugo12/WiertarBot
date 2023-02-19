#!/usr/bin/env sh

set -eux

cp -r static-data/* data/
./app.jar
