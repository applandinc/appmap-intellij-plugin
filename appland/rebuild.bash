#!/bin/bash
set -e

DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)"
cd "$DIR"

npm install
./node_modules/.bin/webpack