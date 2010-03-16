# use JDK1.5 to build native libraries

include VERSION
sqlite := sqlite-$(version)

#CURRENT_DIR = $(shell cygpath -w `pwd`)
RESOURCE_DIR = src/main/resources

ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
  OS := Win
endif
ifeq ($(findstring MINGW,$(shell uname)),MINGW)
  OS := Win
endif
ifeq ($(shell uname),Darwin)
  OS := Darwin
endif

ifeq ($(OS),)
  OS := Default
endif


ifeq ($(arch),)
arch := $(shell uname -m)
endif

target = $(OS)-$(arch)

Default_LIBNAME   := libsqlitejdbc.so
Darwin_LIBNAME   := libsqlitejdbc.jnilib
Win_LIBNAME      := sqlitejdbc.dll
LIBNAME   := $($(OS)_LIBNAME)


.phony: all package win mac linux native deploy

all: package

deploy: 
	mvn deploy 


OSInfoClass=org/sqlite/OSInfo
OSINFO_PROG=target/sqlitejdbc/$(OSInfoClass).class
SQLITE_DLL=sqlitejdbc/build/$(target)/$(LIBNAME)
SQLITE_BUILD_DIR=sqlitejdbc/build/$(sqlite)-$(target)

LIB_FOLDER = $(shell java -cp target/sqlitejdbc $(OSInfoClass))
WORK_DIR=target/dll/$(sqlite)/native
UPDATE_FLAG=target/dll/$(sqlite)/UPDATE

$(OSINFO_PROG): src/main/java/$(OSInfoClass).java
	mkdir -p target/sqlitejdbc
	javac $< -d target/sqlitejdbc

$(SQLITE_DLL): $(SQLITE_BUILD_DIR)

$(SQLITE_BUILD_DIR): Makefile sqlitejdbc/Makefile 
	cd sqlitejdbc && make native 

#$(NATIVE_DLL): $(OSINFO_PROG) $(SQLITE_DLL)
#	mkdir -p $(WORK_DIR)/$(LIB_FOLDER)
#	cp $(SQLITE_DLL) $(WORK_DIR)/$(LIB_FOLDER) 

#native: sqlitejdbc/build/$(target)/$(LIBNAME) target/sqlitejdbc/$(OSInfoClass).class

$(UPDATE_FLAG): $(OSINFO_PROG) $(SQLITE_DLL)
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


