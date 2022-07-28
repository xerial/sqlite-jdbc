#!/bin/bash

# This script prints the commands to upgrade the docker cross compilation scripts

grep -F 'docker run --rm doc' "$(dirname $0)/"dockcross-* | grep -F '>' | \
	sed 's/\:#//' | \
	awk '{print $2" "$3" "$4" "$5" "$6" "$1}'
