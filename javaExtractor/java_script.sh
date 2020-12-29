#!/bin/bash
# shellcheck disable=SC2093
# move to script folder
cd javaExtractor/
# execute jar file to extract wikipedia tables
exec java -jar javaExtractor.jar