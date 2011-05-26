# use JDK1.5 to build native libraries

include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package win mac linux native deploy

all: package

deploy: 
	mvn deploy 

MVN:=mvn
SRC:=src/main/java
SQLITE_OUT:=$(TARGET)/$(sqlite)-$(OS_NAME)-$(OS_ARCH)
SQLITE_ARCHIVE:=$(TARGET)/$(sqlite)-amal.zip
SQLITE_UNPACKED:=$(TARGET)/sqlite-unpack.log
SQLITE_AMAL_DIR=$(TARGET)/$(SQLITE_AMAL_PREFIX)


CFLAGS:= -I$(SQLITE_OUT) -I$(SQLITE_AMAL_DIR) $(CFLAGS)

$(SQLITE_ARCHIVE):
	@mkdir -p $(@D)
	curl -o$@ http://www.sqlite.org/$(SQLITE_AMAL_PREFIX).zip

$(SQLITE_UNPACKED): $(SQLITE_ARCHIVE)
	unzip -qo $< -d $(TARGET)
	touch $@


$(SQLITE_OUT)/org/sqlite/%.class: src/main/java/org/sqlite/%.java
	@mkdir -p $(@D)
	$(JAVAC) -source 1.5 -target 1.5 -sourcepath $(SRC) -d $(SQLITE_OUT) $<

jni-header: $(SRC)/org/sqlite/NativeDB.h

$(SQLITE_OUT)/NativeDB.h: $(SQLITE_OUT)/org/sqlite/NativeDB.class
	$(JAVAH) -classpath $(SQLITE_OUT) -jni -o $@ org.sqlite.NativeDB

test:
	mvn test

clean:
	rm -rf $(WORK)


$(SQLITE_OUT)/sqlite3.o : $(SQLITE_UNPACKED)
	@mkdir -p $(@D)
	perl -p -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(SQLITE_AMAL_DIR)/sqlite3ext.h > $(SQLITE_OUT)/sqlite3ext.h
# insert a code for loading extension functions
	perl -p -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    $(SQLITE_AMAL_DIR)/sqlite3.c > $(SQLITE_OUT)/sqlite3.c
	cat src/main/ext/*.c >> $(SQLITE_OUT)/sqlite3.c
	$(CC) -o $@ -c $(CFLAGS) \
	    -DSQLITE_ENABLE_LOAD_EXTENSION=1 \
	    -DSQLITE_ENABLE_UPDATE_DELETE_LIMIT \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	    -DSQLITE_ENABLE_RTREE \
	    -DSQLITE_ENABLE_STAT2 \
	    $(SQLITE_FLAGS) \
	    $(SQLITE_OUT)/sqlite3.c

$(SQLITE_OUT)/$(LIBNAME): $(SQLITE_OUT)/sqlite3.o $(SRC)/org/sqlite/NativeDB.c $(SQLITE_OUT)/NativeDB.h
	@mkdir -p $(@D)
	$(CC) $(CFLAGS) -c -o $(SQLITE_OUT)/NativeDB.o $(SRC)/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) -o $@ $(SQLITE_OUT)/*.o $(LINKFLAGS)
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


win32: 
	$(MAKE) native CC=mingw32-gcc OS_NAME=Windows OS_ARCH=x86

linux32:
	$(MAKE) native OS_NAME=Linux OS_ARCH=i386


package: native
	rm -rf target/dependency-maven-plugin-markers
	$(MVN) package

clean-native:
	rm -rf $(SQLITE_OUT)


# targets for building pure-java library
purejava: $(SQLITE_BUILD_DIR)/org/sqlite/SQLite.class
	mkdir -p $(RESOURCE_DIR)/org/sqlite
	cp $< $(RESOURCE_DIR)/org/sqlite/SQLite.class

$(TARGET)/classes/org/sqlite/SQLite.class: 
	make -f Makefile.purejava

test-purejava:
	mvn -DargLine="-Dsqlite.purejava=true" test	

