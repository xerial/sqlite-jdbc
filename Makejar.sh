#!/bin/bash
# Used to generate SQLite librairies and make the Java jar archive.

# Make all SQLite libraries
make

# Make the Java jar file
mvn package -Dmaven.test.skip
