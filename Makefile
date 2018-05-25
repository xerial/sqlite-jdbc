
include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package native native-all deploy

all: jni-header package

deploy: 
	mvn package deploy -DperformRelease=true

MVN:=mvn
SRC:=src/main/java
SQLITE_OUT:=$(TARGET)/$(sqlite)-$(OS_NAME)-$(OS_ARCH)
SQLITE_ARCHIVE:=$(TARGET)/$(sqlite)-amal.zip
SQLITE_UNPACKED:=$(TARGET)/sqlite-unpack.log
SQLITE_SOURCE?=$(TARGET)/$(SQLITE_AMAL_PREFIX)
ifneq ($(SQLITE_SOURCE),$(TARGET)/$(SQLITE_AMAL_PREFIX))
	created := $(shell touch $(SQLITE_UNPACKED))
endif

CCFLAGS:= -I$(SQLITE_OUT) -I$(SQLITE_SOURCE) $(CCFLAGS)

$(SQLITE_ARCHIVE):
	@mkdir -p $(@D)
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2018/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2017/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2016/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2015/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2014/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2013/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/$(SQLITE_OLD_AMAL_PREFIX).zip

$(SQLITE_UNPACKED): $(SQLITE_ARCHIVE)
	unzip -qo $< -d $(TARGET)/tmp.$(version)
	(mv $(TARGET)/tmp.$(version)/$(SQLITE_AMAL_PREFIX) $(TARGET) && rmdir $(TARGET)/tmp.$(version)) || mv $(TARGET)/tmp.$(version)/ $(TARGET)/$(SQLITE_AMAL_PREFIX)
	touch $@


$(TARGET)/common-lib/org/sqlite/%.class: src/main/java/org/sqlite/%.java
	@mkdir -p $(@D)
	$(JAVAC) -source 1.5 -target 1.5 -sourcepath $(SRC) -d $(TARGET)/common-lib $<

jni-header: $(TARGET)/common-lib/NativeDB.h

$(TARGET)/common-lib/NativeDB.h: $(TARGET)/common-lib/org/sqlite/core/NativeDB.class
	$(JAVAH) -classpath $(TARGET)/common-lib -jni -o $@ org.sqlite.core.NativeDB

test:
	mvn test

clean: clean-native clean-java clean-tests


$(SQLITE_OUT)/sqlite3.o : $(SQLITE_UNPACKED)
	@mkdir -p $(@D)
	perl -p -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(SQLITE_SOURCE)/sqlite3ext.h > $(SQLITE_OUT)/sqlite3ext.h
# insert a code for loading extension functions
	perl -p -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    $(SQLITE_SOURCE)/sqlite3.c > $(SQLITE_OUT)/sqlite3.c
	cat src/main/ext/*.c >> $(SQLITE_OUT)/sqlite3.c
	$(CC) -o $@ -c $(CCFLAGS) \
	    -DSQLITE_ENABLE_LOAD_EXTENSION=1 \
	    -DSQLITE_HAVE_ISNAN \
	    -DSQLITE_HAVE_USLEEP \
	    -DSQLITE_ENABLE_UPDATE_DELETE_LIMIT \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	    -DSQLITE_ENABLE_FTS5 \
	    -DSQLITE_ENABLE_JSON1 \
	    -DSQLITE_ENABLE_RTREE \
	    -DSQLITE_ENABLE_STAT2 \
	    -DSQLITE_THREADSAFE=1 \
	    -DSQLITE_DEFAULT_MEMSTATUS=0 \
	    -DSQLITE_DEFAULT_FILE_PERMISSIONS=0666 \
	    -DSQLITE_MAX_VARIABLE_NUMBER=250000 \
	    -DSQLITE_MAX_MMAP_SIZE=1099511627776 \
	    $(SQLITE_FLAGS) \
	    $(SQLITE_OUT)/sqlite3.c

$(SQLITE_OUT)/$(LIBNAME): $(SQLITE_OUT)/sqlite3.o $(SRC)/org/sqlite/core/NativeDB.c
	@mkdir -p $(@D)
	$(CC) $(CCFLAGS) -I $(TARGET)/common-lib -c -o $(SQLITE_OUT)/NativeDB.o $(SRC)/org/sqlite/core/NativeDB.c
	$(CC) $(CCFLAGS) -o $@ $(SQLITE_OUT)/*.o $(LINKFLAGS)
# Workaround for strip Protocol error when using VirtualBox on Mac
	cp $@ /tmp/$(@F)
	$(STRIP) /tmp/$(@F)
	cp /tmp/$(@F) $@

NATIVE_DIR=src/main/resources/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

# For cross-compilation, install docker. See also https://github.com/dockcross/dockcross
# Disabled linux-armv6 build because of this issue; https://github.com/dockcross/dockcross/issues/190
native-all: native win32 win64 mac64 linux32 linux64 linux-arm linux-armv7 linux-arm64 linux-android-arm linux-ppc64

native: $(SQLITE_UNPACKED) $(NATIVE_DLL)

$(NATIVE_DLL): $(SQLITE_OUT)/$(LIBNAME)
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)

DOCKER_RUN_OPTS=--rm

win32: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-x86 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=i686-w64-mingw32.static- OS_NAME=Windows OS_ARCH=x86'

win64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-x64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=x86_64-w64-mingw32.static- OS_NAME=Windows OS_ARCH=x86_64'

linux32: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -ti -v $$PWD:/work xerial/centos5-linux-x86 bash -c 'make clean-native native OS_NAME=Linux OS_ARCH=x86'

linux64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -ti -v $$PWD:/work xerial/centos5-linux-x86_64 bash -c 'make clean-native native OS_NAME=Linux OS_ARCH=x86_64'

alpine-linux64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -ti -v $$PWD:/work xerial/alpine-linux-x86_64 bash -c 'make clean-native native OS_NAME=Linux OS_ARCH=x86_64'

linux-arm: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv5 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=arm-linux-gnueabi- OS_NAME=Linux OS_ARCH=arm'

linux-armv6: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv6 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=arm-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armv6'

linux-armv7: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv7 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=arm-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armv7'

linux-arm64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-arm64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/bin/aarch64-unknown-linux-gnueabi/bin/aarch64-unknown-linux-gnueabi- OS_NAME=Linux OS_ARCH=aarch64'

linux-android-arm: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-android-arm -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/arm-linux-androideabi/bin/arm-linux-androideabi- OS_NAME=Linux OS_ARCH=android-arm'

linux-ppc64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-ppc64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=powerpc64le-linux-gnu- OS_NAME=Linux OS_ARCH=ppc64'

mac64: $(SQLITE_UNPACKED) jni-header
	docker run -it $(DOCKER_RUN_OPTS) -v $$PWD:/workdir -e CROSS_TRIPLE=x86_64-apple-darwin multiarch/crossbuild make clean-native native OS_NAME=Mac OS_ARCH=x86_64

# deprecated
mac32: $(SQLITE_UNPACKED) jni-header
	docker run -it $(DOCKER_RUN_OPTS) -v $$PWD:/workdir -e CROSS_TRIPLE=i386-apple-darwin multiarch/crossbuild make clean-native native OS_NAME=Mac OS_ARCH=x86

sparcv9:
	$(MAKE) native OS_NAME=SunOS OS_ARCH=sparcv9

package: native-all
	rm -rf target/dependency-maven-plugin-markers
	$(MVN) package

clean-native:
	rm -rf $(SQLITE_OUT)

clean-java:
	rm -rf $(TARGET)/*classes
	rm -rf $(TARGET)/sqlite-jdbc-*jar

clean-tests:
	rm -rf $(TARGET)/{surefire*,testdb.jar*}

docker-linux64:
	docker build -f docker/Dockerfile.linux_x86_64 -t xerial/centos5-linux-x86_64 .

docker-linux32:
	docker build -f docker/Dockerfile.linux_x86 -t xerial/centos5-linux-x86 .

docker-alpine-linux64:
	docker build -f docker/Dockerfile.alpine-linux_x86_64 -t xerial/alpine-linux-x86_64 .
