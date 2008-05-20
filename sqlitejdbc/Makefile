# Makefile for the native SQLite JDBC Driver
#
# Tested on Mac OS X, Linux and Windows/Cygwin. This file generates a jar file
# and native jnilib/so/dll library that provides an SQLite JDBC driver.
#
# Author:    David Crawshaw, 2007
# License:   New BSD (see LICENSE file)
#
# No auto-goop. Just try typin 'make'.
#

include Makefile.common

default: test

dl/$(sqlite)-amal.zip:
	@mkdir -p dl
	curl -odl/$(sqlite)-amal.zip \
	    http://www.sqlite.org/sqlite-amalgamation-$(subst .,_,$(sqlite_version)).zip

build/$(sqlite)-%/sqlite3.o: dl/$(sqlite)-amal.zip
	@mkdir -p build/$(sqlite)-$*
	unzip -qo dl/$(sqlite)-amal.zip -d build/$(sqlite)-$*
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    build/$(sqlite)-$*/sqlite3ext.h
	(cd build/$(sqlite)-$*; $(CC) -o sqlite3.o -c $(CFLAGS) \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_OMIT_LOAD_EXTENSION sqlite3.c)

build/org/%.class: src/org/%.java
	@mkdir -p build
	$(JAVAC) -source 1.2 -target 1.2 -sourcepath src -d build $<

build/test/%.class: src/test/%.java
	@mkdir -p build
	$(JAVAC) -target 1.5 -classpath "build$(sep)$(libjunit)" \
	    -sourcepath src/test -d build $<

native: build/$(sqlite)-$(target)/sqlite3.o $(native_classes)
	@mkdir -p build/$(target)
	$(JAVAH) -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	cd build && jar cf $(sqlitejdbc)-native.jar $(java_classlist)
	$(CC) $(CFLAGS) -c -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o build/$(sqlite)-$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

dist/$(sqlitejdbc)-$(target).tgz: native
	@mkdir -p dist
	tar cfz dist/$(sqlitejdbc)-$(target).tgz README \
	    -C build $(sqlitejdbc)-native.jar -C $(target) $(LIBNAME)

test: native $(test_classes)
	$(JAVA) -Djava.library.path=build/$(target) \
	    -cp "build/$(sqlitejdbc)-native.jar$(sep)build$(sep)$(libjunit)" \
	    org.junit.runner.JUnitCore $(tests)

clean:
	rm -rf build
	rm -rf dist
