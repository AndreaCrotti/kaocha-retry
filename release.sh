#!/usr/bin/env bash

rm -f pom.xml
clj -A:release $@
