#!/usr/bin/env bash
# Used to generate the version for the amalgamation download zip.
# https://www.sqlite.org/download.html#encoding
# The version is encoded so that filenames sort in order of increasing version number when viewed using "ls".
# For version 3.X.Y the filename encoding is 3XXYY00. For branch version 3.X.Y.Z, the encoding is 3XXYYZZ.
version=""
i=0
export IFS="."
for num in $1; do
	if [ $i -gt 0 ]; then
		if [ $num -le 9 ]; then
			eval num=0$num
		fi
	fi
	eval version=$version$num
	let i+=1
done
unset IFS
if [ $i -gt 3 ]; then
  echo "$version"
else
  echo "$version"00
fi
