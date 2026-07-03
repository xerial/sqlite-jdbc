#!/bin/sh
# Used to generate the version for the amalgamation download zip.
# https://www.sqlite.org/download.html#encoding
# The version is encoded so that filenames sort in order of increasing version number when viewed using "ls".
# For version 3.X.Y the filename encoding is 3XXYY00. For branch version 3.X.Y.Z, the encoding is 3XXYYZZ.
version=""
i=0
export IFS="."
for num in $1; do
  if [ $i -gt 0 ]; then
    case $num in
      [0-9]) num=0$num ;;
    esac
  fi
  version="${version}${num}"
  i=$((i+1))
done
unset IFS
if [ $i -gt 3 ]; then
  echo "$version"
else
  echo "$version"00
fi
