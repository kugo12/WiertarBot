#!/usr/bin/env sh

set -eux

cp -r static-data/* data/
pypy3 -m WiertarBot
