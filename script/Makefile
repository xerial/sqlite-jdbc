# Makefile for the native SQLite JDBC Driver
#
# No auto-goop. Just try typing 'make'. You should get two interesting files:
#     build/TARGET_OS/LIBNAME
#     build/sqlitejdbc-vXXX-native.jar
#
# To combine these, type:
# 	  cd build
# 	  mv LIBNAME linux-x86.lib (or win-x86.lib, freebsd-ppc.lib, mac.lib, etc)
#     java uf sqlitejdbc-vXXX-native.jar linux-x86.lib
#
# The first is the native library, the second is the java support files.
# Generating the more complete sqlitejdbc-vXXX.jar requires building the
# NestedVM source, which requires running a Linux machine, and looking at
# the other make files.
#

include Makefile.common

default: test

test: native $(test_classes)
	$(JAVA) -Djava.library.path=build/$(target) \
	    -cp "build/$(sqlitejdbc)-native.jar$(sep)build$(sep)$(libjunit)" \
	    org.junit.runner.JUnitCore $(tests)

native: build/$(sqlitejdbc)-native.jar build/$(target)/$(LIBNAME)

native: build/$(sqlite)-$(target)/sqlite3.o $(native_classes)
	@mkdir -p build/$(target)
	$(JAVAH) -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	cd build && jar cf $(sqlitejdbc)-native.jar $(java_classlist)
	$(CC) $(CFLAGS) -c -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o build/$(sqlite)-$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)
	
build/$(sqlitejdbc)-native.jar: $(native_classes)
	cd build && jar cf $(sqlitejdbc)-native.jar $(java_classlist)

build/$(target)/$(LIBNAME): build/$(sqlite)-$(target)/sqlite3.o build/org/sqlite/NativeDB.class
	@mkdir -p build/$(target)
	$(JAVAH) -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	$(CC) $(CFLAGS) -c -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o build/$(sqlite)-$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

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

dl/$(sqlite)-amal.zip:
	@mkdir -p dl
	curl -odl/$(sqlite)-amal.zip \
	http://www.sqlite.org/sqlite-amalgamation-$(subst .,_,$(sqlite_version)).zip

clean:
	rm -rf build dist







