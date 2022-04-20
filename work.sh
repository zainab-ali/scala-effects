#!/bin/bash

set -ex

path="$1"
count="$2"

for (( c=1; c<=$count; c++ ))
do
    curl --verbose http://localhost:8081/$path &
done

