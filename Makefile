# use JDK1.5 to build native libraries

include Makefile.version

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

all: native

deploy: 
	mvn deploy 


OSInfoClass=org/xerial/db/sql/sqlite/OSInfo


target/sqlitejdbc/$(OSInfoClass).class:
	mkdir -p target/sqlitejdbc
	javac src/main/java/$(OSInfoClass).java -d target/sqlitejdbc

sqlitejdbc/build/$(target)/$(LIBNAME): 
	cd sqlitejdbc && make native 

sqlitejdbc/build/$(sqlite)-$(target):
	cd sqlitejdbc && make native

LIB_FOLDER = $(RESOURCE_DIR)/native/$(shell java -cp target/sqlitejdbc org.xerial.db.sql.sqlite.OSInfo)

#native: sqlitejdbc/build/$(target)/$(LIBNAME) target/sqlitejdbc/$(OSInfoClass).class
native: sqlitejdbc/build/$(sqlite)-$(target) sqlitejdbc/build/$(target)/$(LIBNAME) target/sqlitejdbc/$(OSInfoClass).class
	mkdir -p $(LIB_FOLDER)
	cp sqlitejdbc/build/$(target)/$(LIBNAME) $(LIB_FOLDER) 
	mvn package

purejava: 
	cd sqlitejdbc && make -f Makefile.nested
	mkdir -p $(RESOURCE_DIR)/org/sqlite
	cp sqlitejdbc/build/org/sqlite/SQLite.class $(RESOURCE_DIR)/org/sqlite/

clean:
	cd sqlitejdbc && make clean
	rm -rf sqlitejdbc/dl
	mvn clean


