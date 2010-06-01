# use JDK1.5 to build native libraries

include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package win mac linux native deploy

all: package

deploy: 
	mvn deploy 

SQLITE_DLL=sqlitejdbc/build/$(target)/$(LIBNAME)
SQLITE_BUILD_DIR=sqlitejdbc/build/$(sqlite)-$(target)

WORK_DIR=target/dll/$(sqlite)/native
UPDATE_FLAG=target/dll/$(sqlite)/UPDATE

$(SQLITE_DLL): $(SQLITE_BUILD_DIR)

$(SQLITE_BUILD_DIR): Makefile.native
	$(MAKE) -f $< native 

$(UPDATE_FLAG): $(SQLITE_DLL)
	mkdir -p $(WORK_DIR)/$(LIB_FOLDER)
	cp $(SQLITE_DLL) $(WORK_DIR)/$(LIB_FOLDER) 
	mkdir -p $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	cp $(NATIVE_DLL) $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	touch $(UPDATE_FLAG)

native: $(UPDATE_FLAG)

NATIVE_DLL=$(WORK_DIR)/$(LIB_FOLDER)/$(LIBNAME)

package: $(UPDATE_FLAG)
	mkdir -p $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	cp $(NATIVE_DLL) $(RESOURCE_DIR)/native/$(LIB_FOLDER)
	rm -rf target/dependency-maven-plugin-markers
	mvn package

clean-native:
	rm -rf sqlitejdbc/build/$(sqlite)-$(target) $(UPDATE_FLAG)

purejava: 
	cd sqlitejdbc && make -f Makefile.nested
	mkdir -p $(RESOURCE_DIR)/org/sqlite
	cp sqlitejdbc/build/org/sqlite/SQLite.class $(RESOURCE_DIR)/org/sqlite/

test-purejava:
	mvn -DargLine="-Dsqlite.purejava=true" test	

clean:
	cd sqlitejdbc && make clean
	rm -rf sqlitejdbc/dl
	mvn clean


