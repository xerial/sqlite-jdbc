# use JDK1.5 to build native libraries

include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package win32 mac32 linux32 native deploy

all: package

deploy: 
	mvn deploy 

MVN:=mvn
SRC:=src/main/java
SQLITE_OUT:=$(TARGET)/$(sqlite)-$(OS_NAME)-$(OS_ARCH)
SQLITE_ARCHIVE:=$(TARGET)/$(sqlite)-amal.zip
SQLITE_UNPACKED:=$(TARGET)/sqlite-unpack.log
SQLITE_AMAL_DIR=$(TARGET)/$(SQLITE_AMAL_PREFIX)


CCFLAGS:= -I$(SQLITE_OUT) -I$(SQLITE_AMAL_DIR) $(CCFLAGS)

$(SQLITE_ARCHIVE):
	@mkdir -p $(@D)
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/2016/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/2015/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/2014/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/2013/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ http://www.sqlite.org/$(SQLITE_OLD_AMAL_PREFIX).zip

$(SQLITE_UNPACKED): $(SQLITE_ARCHIVE)
	unzip -qo $< -d $(TARGET)/tmp.$(version)
	(mv $(TARGET)/tmp.$(version)/$(SQLITE_AMAL_PREFIX) $(TARGET) && rmdir $(TARGET)/tmp.$(version)) || mv $(TARGET)/tmp.$(version)/ $(TARGET)/$(SQLITE_AMAL_PREFIX)
	touch $@


$(SQLITE_OUT)/org/sqlite/%.class: src/main/java/org/sqlite/%.java
	@mkdir -p $(@D)
	$(JAVAC) -source 1.5 -target 1.5 -sourcepath $(SRC) -d $(SQLITE_OUT) $<

jni-header: $(SQLITE_OUT)/NativeDB.h

$(SQLITE_OUT)/NativeDB.h: $(SQLITE_OUT)/org/sqlite/core/NativeDB.class
	$(JAVAH) -classpath $(SQLITE_OUT) -jni -o $@ org.sqlite.core.NativeDB

test:
	mvn test

clean: clean-native clean-java clean-tests


$(SQLITE_OUT)/sqlite3.o : $(SQLITE_UNPACKED)
	@mkdir -p $(@D)
	perl -p -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(SQLITE_AMAL_DIR)/sqlite3ext.h > $(SQLITE_OUT)/sqlite3ext.h
# insert a code for loading extension functions
	perl -p -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    $(SQLITE_AMAL_DIR)/sqlite3.c > $(SQLITE_OUT)/sqlite3.c
	cat src/main/ext/*.c >> $(SQLITE_OUT)/sqlite3.c
	$(CC) -o $@ -c $(CCFLAGS) \
	    -DSQLITE_ENABLE_LOAD_EXTENSION=1 \
	    -DSQLITE_HAVE_ISNAN \
	    -DSQLITE_ENABLE_UPDATE_DELETE_LIMIT \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	    -DSQLITE_ENABLE_FTS5 \
	    -DSQLITE_ENABLE_JSON1 \
	    -DSQLITE_ENABLE_RTREE \
	    -DSQLITE_ENABLE_STAT2 \
	    -DSQLITE_THREADSAFE=0 \
	    $(SQLITE_FLAGS) \
	    $(SQLITE_OUT)/sqlite3.c

$(SQLITE_OUT)/$(LIBNAME): $(SQLITE_OUT)/sqlite3.o $(SRC)/org/sqlite/core/NativeDB.c $(SQLITE_OUT)/NativeDB.h
	@mkdir -p $(@D)
	$(CC) $(CCFLAGS) -c -o $(SQLITE_OUT)/NativeDB.o $(SRC)/org/sqlite/core/NativeDB.c
	$(CC) $(CCFLAGS) -o $@ $(SQLITE_OUT)/*.o $(LINKFLAGS)
	$(STRIP) $@


NATIVE_DIR=src/main/resources/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

native: $(SQLITE_UNPACKED) $(NATIVE_DLL)

$(NATIVE_DLL): $(SQLITE_OUT)/$(LIBNAME)
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)

# for cross-compilation on Ubuntu, install the g++-mingw-w64-i686 package
win32: 
	$(MAKE) native CROSS_PREFIX=i686-w64-mingw32- OS_NAME=Windows OS_ARCH=x86

# for cross-compilation on Ubuntu, install the g++-mingw-w64-x86-64 package
win64: 
	$(MAKE) native CROSS_PREFIX=x86_64-w64-mingw32- OS_NAME=Windows OS_ARCH=x86_64

linux32:
	$(MAKE) native OS_NAME=Linux OS_ARCH=x86

linux64:
	$(MAKE) native OS_NAME=Linux OS_ARCH=x86_64

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabi package
linux-arm:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabi- OS_NAME=Linux OS_ARCH=arm

# for cross-compilation on Ubuntu, install the g++-arm-linux-gnueabihf package
linux-armhf:
	$(MAKE) native CROSS_PREFIX=arm-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armhf

sparcv9:
	$(MAKE) native OS_NAME=SunOS OS_ARCH=sparcv9


mac32:
	$(MAKE) native OS_NAME=Mac OS_ARCH=x86


package: $(NATIVE32_DLL) native
	rm -rf target/dependency-maven-plugin-markers
	$(MVN) package

clean-native:
	rm -rf $(TARGET)/$(sqlite)-$(OS_NAME)*

clean-java:
	rm -rf $(TARGET)/*classes
	rm -rf $(TARGET)/sqlite-jdbc-*jar

clean-tests:
	rm -rf $(TARGET)/{surefire*,testdb.jar*}
