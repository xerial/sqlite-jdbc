#!/bin/bash

VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)

# Deploy a snapshot version only for master branch and jdk8
if [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ]; then 
  if [ "$TRAVIS_PULL_REQUEST" == "false" -a "$VERSION" == *SNAPSHOT ]; then
     echo "make && mvn -s settings.xml deploy -DskipTests";
  else
    echo "make";
  fi;
else
  echo "make linux64 && mvn test";
fi
