# use JDK1.5 to build native libraries

include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package win mac linux native deploy

all: package

deploy: 
	mvn deploy 

WORK:=target
WORK_DIR=$(WORK)/dll/$(sqlite)/native
UPDATE_FLAG=$(WORK)/dll/$(sqlite)/UPDATE
NATIVE_BUILD:=$(WORK)/build
NATIVE_DLL:=$(WORK_DIR)/$(LIB_FOLDER)/$(LIBNAME)

SQLITE_DLL=$(NATIVE_BUILD)/$(target)/$(LIBNAME)
SQLITE_BUILD_DIR=$(NATIVE_BUILD)/$(sqlite)-$(target)


$(UPDATE_FLAG): $(SQLITE_DLL)
	mkdir -p $(WORK_DIR)/$(LIB_FOLDER)
	cp $(SQLITE_DLL) $(WORK_DIR)/$(LIB_FOLDER) 
	mkdir -p $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	cp $(NATIVE_DLL) $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	touch $(UPDATE_FLAG)

native: $(UPDATE_FLAG)

package: $(UPDATE_FLAG)
	rm -rf target/dependency-maven-plugin-markers
	mvn package

clean-native:
	rm -rf $(SQLITE_BUILD_DIR) $(UPDATE_FLAG)

purejava: 
	cd sqlitejdbc && make -f Makefile.nested
	mkdir -p $(RESOURCE_DIR)/org/sqlite
	cp sqlitejdbc/build/org/sqlite/SQLite.class $(RESOURCE_DIR)/org/sqlite/

test-purejava:
	mvn -DargLine="-Dsqlite.purejava=true" test	

test:
	mvn test

clean:
	rm -rf $(WORK)



$(SQLITE_DLL): $(SQLITE_BUILD_DIR)/sqlite3.o $(NATIVE_BUILD)/org/sqlite/NativeDB.class src/main/java/org/sqlite/NativeDB.c
	@mkdir -p $(dir $@)
	$(JAVAH) -classpath $(NATIVE_BUILD) -jni \
		-o $(NATIVE_BUILD)/NativeDB.h org.sqlite.NativeDB
	$(CC) $(CFLAGS) -c -o $(NATIVE_BUILD)/$(target)/NativeDB.o \
		src/main/java/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o $@ \
		$(NATIVE_BUILD)/$(target)/NativeDB.o $(SQLITE_BUILD_DIR)/*.o 
	$(STRIP) $@

$(NATIVE_BUILD)/$(sqlite)-%/sqlite3.o: $(WORK)/dl/$(sqlite)-amal.zip
	@mkdir -p $(dir $@)
	$(info building a native library for os:$(OS_NAME) arch:$(OS_ARCH))
	unzip -qo $(WORK)/dl/$(sqlite)-amal.zip -d $(NATIVE_BUILD)/$(sqlite)-$*
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(NATIVE_BUILD)/$(sqlite)-$*/sqlite3ext.h
# insert a code for loading extension functions
	perl -pi -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    $(NATIVE_BUILD)/$(sqlite)-$*/sqlite3.c
	cat sqlitejdbc/ext/*.c >> $(NATIVE_BUILD)/$(sqlite)-$*/sqlite3.c
	(cd $(NATIVE_BUILD)/$(sqlite)-$*; $(CC) -o sqlite3.o -c $(CFLAGS) \
	    -DSQLITE_ENABLE_LOAD_EXTENSION \
	    -DSQLITE_ENABLE_UPDATE_DELETE_LIMIT \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	    -DSQLITE_ENABLE_RTREE \
	    -DSQLITE_ENABLE_STAT2 \
	    $(SQLITE_FLAGS) \
	    sqlite3.c)

$(NATIVE_BUILD)/org/sqlite/%.class: src/main/java/org/sqlite/%.java
	@mkdir -p $(NATIVE_BUILD)
	$(JAVAC) -source 1.5 -target 1.5 -sourcepath src/main/java -d $(NATIVE_BUILD) $<

$(WORK)/dl/$(sqlite)-amal.zip:
	@mkdir -p $(dir $@)
	curl -o$@ \
	http://www.sqlite.org/sqlite-amalgamation-$(subst .,_,$(version)).zip


