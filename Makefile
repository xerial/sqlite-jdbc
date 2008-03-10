
CURRENT_DIR = $(shell cygpath -w `pwd`)

# use JDK1.5 to build native libraries

.phony: all package win mac linux native

all: package 

package: 
	mvn clean package deploy #-Dmaven.test.skip=true

LIBDIR=src/main/resources/native

#win: $(LIBDIR)/win/sqlitejdbc.dll
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/win/md5sum)

#mac: $(LIBDIR)/win/sqlitejdbc.dll
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/mac/md5sum)

#linux: $(LIBDIR)/linux/libsqlitejdbc.so
#	$(shell md5sum -b $< | cut -f 1 -d ' ' > $(LIBDIR)/linux/md5sum)


native:
	cd sqlitejdbc && make native test

clean:
	mvn clean
