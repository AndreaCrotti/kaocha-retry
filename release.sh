#!/usr/bin/env bash

rm pom.xml
clj -A:release $@
