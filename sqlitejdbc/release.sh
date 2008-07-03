#!/bin/sh
#
# Compile and prepare for distribution Mac/Win/Java versions.
# Expects to run on Mac OS X with the DarwinPorts windows cross compiler.
#

sqlitejdbc="sqlitejdbc-v`cat VERSION`"
afs="/afs/hcoop.net/user/c/cr/crawshaw/web/zentus/sqlitejdbc"
repo="$afs/src"

#
# bundle source code
#
echo '*** bundling source ***'
echo $repo > _darcs/prefs/defaultrepo
mkdir -p dist
mkdir -p work/$sqlitejdbc/src
cp Makefile* work/$sqlitejdbc/.
cp README work/$sqlitejdbc/.
cp LICENSE work/$sqlitejdbc/.
cp VERSION work/$sqlitejdbc/.
cp -R src/org work/$sqlitejdbc/src/.
cp -R src/test work/$sqlitejdbc/src/.
cp -R _darcs work/$sqlitejdbc/.
cp -R lib work/$sqlitejdbc/.
(cd work && tar cfz ../dist/$sqlitejdbc-src.tgz $sqlitejdbc)
rm -rf work

#
# universal binary
#
maclib=libsqlitejdbc.jnilib

echo '*** compiling for mac/ppc ***'
make os=Darwin arch=ppc native

echo '*** compiling for mac/i386 ***'
make os=Darwin arch=i386 native

echo '*** lipo ppc and i386 ***'
mkdir -p build/Darwin-universal
lipo -create build/Darwin-ppc/$maclib \
             build/Darwin-i386/$maclib \
     -output build/Darwin-universal/$maclib
mkdir -p dist
tar cfz dist/$sqlitejdbc-Mac.tgz README \
    -C build $sqlitejdbc-native.jar \
    -C Darwin-universal $maclib

#
# windows
#
echo '*** compiling for windows ***'
darcs push -a debian:repo/sqlitejdbc
ssh debian "cd repo/sqlitejdbc && make os=Win arch=i586 dist/$sqlitejdbc-Win-i586.tgz"
scp debian:repo/sqlitejdbc/dist/$sqlitejdbc-Win-i586.tgz \
    dist/$sqlitejdbc-Win-i586.tgz

#
# linux
#
echo '*** compiling for linux ***'
ssh debian "cd repo/sqlitejdbc && make arch=i386 dist/$sqlitejdbc-Default-i386.tgz"
scp debian:repo/sqlitejdbc/dist/$sqlitejdbc-Default-i386.tgz \
    dist/$sqlitejdbc-Linux-i386.tgz

#
# pure java compile
#
echo '*** compiling pure java ***'
ssh debian "cd repo/sqlitejdbc && make -f Makefile.nested test dist/$sqlitejdbc-nested.tgz"
scp debian:repo/sqlitejdbc/dist/$sqlitejdbc-nested.tgz \
    dist/$sqlitejdbc-nested.tgz

#
# build changes.html
#
echo '*** building changes.html ***'
cat > changes.html << EOF
<html>
<head>
<link rel="stylesheet" type="text/css" href="/content.css" />
<title>SQLiteJDBC - Changelog</title>
</head>
<body>
EOF
cat web/ad.inc >> changes.html
echo '<div class="content"><h1>Changelog</h1>' >> changes.html
cat web/nav.inc >> changes.html
echo '<h3>HEAD</h3><ul>' >> changes.html
# do not go back before version 008
sh -c 'darcs changes --from-patch="version 026"' | grep \* >> changes.html
perl -pi -e "s/^  \* version ([0-9]+)$/<\/ul><h3>Version \$1<\/h3><ul>/g" \
	changes.html
perl -pi -e "s/^  \* (.*)\$/<li>\$1<\/li>/g" changes.html
echo '</ul></div></body></html>' >> changes.html

#
# push release to web server
#
if [ "$1" = "push" ]; then
    echo '*** pushing release to afs ***'
    darcs push -a $repo
    cp dist/$sqlitejdbc-*.tgz $afs/dist/
    cp changes.html web/*.html web/*.css $afs/
    rm changes.html
fi
