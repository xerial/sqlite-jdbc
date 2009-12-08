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

SRC_FOLDER=../src/main/java/org/sqlite/

default: test

test: native 

native: build/$(target)/$(LIBNAME)
	

build/$(target)/$(LIBNAME): build/$(sqlite)-$(target)/sqlite3.o build/org/sqlite/NativeDB.class ../src/main/java/org/sqlite/NativeDB.c
	@mkdir -p build/$(target)
	$(JAVAH) -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	$(CC) $(CFLAGS) -c -o build/$(target)/NativeDB.o \
		../src/main/java/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o build/$(sqlite)-$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

build/$(sqlite)-%/sqlite3.o: dl/$(sqlite)-amal.zip
	@mkdir -p build/$(sqlite)-$*
	unzip -qo dl/$(sqlite)-amal.zip -d build/$(sqlite)-$*
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    build/$(sqlite)-$*/sqlite3ext.h
# insert a code for loading extension functions
	perl -pi -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    build/$(sqlite)-$*/sqlite3.c
	cat ext/*.c >> build/$(sqlite)-$*/sqlite3.c
	(cd build/$(sqlite)-$*; $(CC) -o sqlite3.o -c $(CFLAGS) \
		-DSQLITE_ENABLE_UPDATE_DELETE_LIMIT \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
		-DSQLITE_ENABLE_RTREE \
	    sqlite3.c)

build/org/sqlite/%.class: ../src/main/java/org/sqlite/%.java
	@mkdir -p build
	$(JAVAC) -source 1.5 -target 1.5 -sourcepath ../src/main/java -d build $<

build/test/org/sqlite/%.class: ../src/test/java/org/sqlite/%.java
	@mkdir -p build/test
	$(JAVAC) -target 1.5 -classpath "build$(sep)$(libjunit)" \
	    -sourcepath ../src/test/java -d build/test $<

dl/$(sqlite)-amal.zip:
	@mkdir -p dl
	curl -odl/$(sqlite)-amal.zip \
	http://www.sqlite.org/sqlite-amalgamation-$(subst .,_,$(version)).zip

clean:
	rm -rf build dist







