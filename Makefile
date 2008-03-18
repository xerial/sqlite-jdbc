# use JDK1.5 to build native libraries

CURRENT_DIR = $(shell cygpath -w `pwd`)
RESOURCE_DIR = src/main/resources/native


ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
  LIB_FOLDER := $(RESOURCE_DIR)/win
  OS := Win
endif
ifeq ($(findstring MINGW,$(shell uname)),MINGW)
  LIB_FOLDER := $(RESOURCE_DIR)/win
  OS := Win
endif
ifeq ($(shell uname),Darwin)
  LIB_FOLDER := $(RESOURCE_DIR)/mac
  OS := Darwin
endif

ifeq ($(OS),)
  LIB_FOLDER := $(RESOURCE_DIR)/linux
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


#win: $(LIBDIR)/win/sqlitejdbc.dll
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/win/md5sum)

#mac: $(LIBDIR)/win/sqlitejdbc.dll
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/mac/md5sum)

#linux: $(LIBDIR)/linux/libsqlitejdbc.so
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/linux/md5sum)


sqlitejdbc/build/$(target)/$(LIBNAME):
	cd sqlitejdbc && make native 

native: sqlitejdbc/build/$(target)/$(LIBNAME)
	mkdir -p $(LIB_FOLDER)
	cp sqlitejdbc/build/$(target)/$(LIBNAME) $(LIB_FOLDER) 
	mvn package


clean:
	cd sqlitejdbc && make clean
	rm -rf sqlitejdbc/dl
	mvn clean

