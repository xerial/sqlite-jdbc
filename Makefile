
include Makefile.common

RESOURCE_DIR = src/main/resources

.phony: all package native native-all deploy

all: jni-header package

deploy: 
	mvn package deploy -DperformRelease=true

DOCKER_RUN_OPTS=--rm
MVN:=mvn
CODESIGN:=docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir gotson/rcodesign sign
SRC:=src/main/java
JAVA_CLASSPATH:=$(TARGET)/classpath/slf4j-api.jar
SQLITE_OUT:=$(TARGET)/$(sqlite)-$(OS_NAME)-$(OS_ARCH)
SQLITE_OBJ?=$(SQLITE_OUT)/sqlite3.o
SQLITE_ARCHIVE:=$(TARGET)/$(sqlite)-amal.zip
SQLITE_UNPACKED:=$(TARGET)/sqlite-unpack.log
SQLITE_SOURCE?=$(TARGET)/$(SQLITE_AMAL_PREFIX)
SQLITE_HEADER?=$(SQLITE_SOURCE)/sqlite3.h
ifneq ($(SQLITE_SOURCE),$(TARGET)/$(SQLITE_AMAL_PREFIX))
	created := $(shell touch $(SQLITE_UNPACKED))
endif

SQLITE_INCLUDE := $(shell dirname "$(SQLITE_HEADER)")

CCFLAGS:= -I$(SQLITE_OUT) -I$(SQLITE_INCLUDE) $(CCFLAGS)

$(SQLITE_ARCHIVE):
	@mkdir -p $(@D)
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2024/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2023/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2022/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2021/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/2020/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/$(SQLITE_AMAL_PREFIX).zip || \
	curl -L --max-redirs 0 -f -o$@ https://www.sqlite.org/$(SQLITE_OLD_AMAL_PREFIX).zip

$(SQLITE_UNPACKED): $(SQLITE_ARCHIVE)
	unzip -qo $< -d $(TARGET)/tmp.$(version)
	(mv $(TARGET)/tmp.$(version)/$(SQLITE_AMAL_PREFIX) $(TARGET) && rmdir $(TARGET)/tmp.$(version)) || mv $(TARGET)/tmp.$(version)/ $(TARGET)/$(SQLITE_AMAL_PREFIX)
	touch $@

$(JAVA_CLASSPATH):
	@mkdir -p $(@D)
	curl -L -f -o$@ https://search.maven.org/remotecontent?filepath=org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar

$(TARGET)/common-lib/org/sqlite/%.class: src/main/java/org/sqlite/%.java
	@mkdir -p $(@D)
	$(JAVAC) -source 1.6 -target 1.6 -sourcepath $(SRC) -d $(TARGET)/common-lib $<

jni-header: $(TARGET)/common-lib/NativeDB.h

$(TARGET)/common-lib/NativeDB.h: src/main/java/org/sqlite/core/NativeDB.java $(JAVA_CLASSPATH)
	@mkdir -p $(TARGET)/common-lib
	$(JAVAC) -cp $(JAVA_CLASSPATH) -d $(TARGET)/common-lib -sourcepath $(SRC) -h $(TARGET)/common-lib src/main/java/org/sqlite/core/NativeDB.java
	mv target/common-lib/org_sqlite_core_NativeDB.h target/common-lib/NativeDB.h

test:
	mvn test

clean: clean-native clean-java clean-tests


$(SQLITE_OUT)/sqlite3.o : $(SQLITE_UNPACKED)
	@mkdir -p $(@D)
	perl -p -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(SQLITE_SOURCE)/sqlite3ext.h > $(SQLITE_OUT)/sqlite3ext.h
# insert a code for loading extension functions
	perl -p -e "s/^opendb_out:/  if(!db->mallocFailed && rc==SQLITE_OK){ rc = RegisterExtensionFunctions(db); }\nopendb_out:/;" \
	    $(SQLITE_SOURCE)/sqlite3.c > $(SQLITE_OUT)/sqlite3.c.tmp
# register compile option 'JDBC_EXTENSIONS'
# limits defined here: https://www.sqlite.org/limits.html
	perl -p -e "s/^(static const char \* const sqlite3azCompileOpt.+)$$/\1\n\n\/* This has been automatically added by sqlite-jdbc *\/\n  \"JDBC_EXTENSIONS\",/;" \
	    $(SQLITE_OUT)/sqlite3.c.tmp > $(SQLITE_OUT)/sqlite3.c
	cat src/main/ext/*.c >> $(SQLITE_OUT)/sqlite3.c
	$(CC) -o $@ -c $(CCFLAGS) \
	    -DSQLITE_ENABLE_LOAD_EXTENSION=1 \
	    -DSQLITE_HAVE_ISNAN \
	    -DHAVE_USLEEP=1 \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS3 \
	    -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	    -DSQLITE_ENABLE_FTS5 \
	    -DSQLITE_ENABLE_RTREE \
	    -DSQLITE_ENABLE_STAT4 \
	    -DSQLITE_ENABLE_DBSTAT_VTAB \
	    -DSQLITE_ENABLE_MATH_FUNCTIONS \
	    -DSQLITE_THREADSAFE=1 \
	    -DSQLITE_DEFAULT_MEMSTATUS=0 \
	    -DSQLITE_DEFAULT_FILE_PERMISSIONS=0666 \
	    -DSQLITE_MAX_VARIABLE_NUMBER=250000 \
	    -DSQLITE_MAX_MMAP_SIZE=1099511627776 \
	    -DSQLITE_MAX_LENGTH=2147483647 \
	    -DSQLITE_MAX_COLUMN=32767 \
	    -DSQLITE_MAX_SQL_LENGTH=1073741824 \
	    -DSQLITE_MAX_FUNCTION_ARG=127 \
	    -DSQLITE_MAX_ATTACHED=125 \
	    -DSQLITE_MAX_PAGE_COUNT=4294967294 \
	    -DSQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS \
	    $(SQLITE_FLAGS) \
	    $(SQLITE_OUT)/sqlite3.c

$(SQLITE_SOURCE)/sqlite3.h: $(SQLITE_UNPACKED)

$(SQLITE_OUT)/$(LIBNAME): $(SQLITE_HEADER) $(SQLITE_OBJ) $(SRC)/org/sqlite/core/NativeDB.c $(TARGET)/common-lib/NativeDB.h
	@mkdir -p $(@D)
	$(CC) $(CCFLAGS) -I $(TARGET)/common-lib -c -o $(SQLITE_OUT)/NativeDB.o $(SRC)/org/sqlite/core/NativeDB.c
	$(CC) $(CCFLAGS) -o $@ $(SQLITE_OUT)/NativeDB.o $(SQLITE_OBJ) $(LINKFLAGS)
# Workaround for strip Protocol error when using VirtualBox on Mac
	cp $@ /tmp/$(@F)
	$(STRIP) /tmp/$(@F)
	cp /tmp/$(@F) $@

NATIVE_DIR=src/main/resources/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_TARGET_DIR:=$(TARGET)/classes/org/sqlite/native/$(OS_NAME)/$(OS_ARCH)
NATIVE_DLL:=$(NATIVE_DIR)/$(LIBNAME)

# For cross-compilation, install docker. See also https://github.com/dockcross/dockcross
native-all: native win32 win64 win-armv7 win-arm64 mac64-signed mac-arm64-signed linux32 linux64 freebsd32 freebsd64 freebsd-arm64 linux-arm linux-armv6 linux-armv7 linux-arm64 linux-android-arm linux-android-arm64 linux-android-x86 linux-android-x64 linux-ppc64 linux-musl32 linux-musl64 linux-musl-arm64

native: $(NATIVE_DLL)

$(NATIVE_DLL): $(SQLITE_OUT)/$(LIBNAME)
	@mkdir -p $(@D)
	cp $< $@
	@mkdir -p $(NATIVE_TARGET_DIR)
	cp $< $(NATIVE_TARGET_DIR)/$(LIBNAME)

win32: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-x86 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=i686-w64-mingw32.static- OS_NAME=Windows OS_ARCH=x86'

win64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-x64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=x86_64-w64-mingw32.static- OS_NAME=Windows OS_ARCH=x86_64'

win-armv7: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-armv7 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=armv7-w64-mingw32- OS_NAME=Windows OS_ARCH=armv7'

win-arm64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-windows-arm64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=aarch64-w64-mingw32- OS_NAME=Windows OS_ARCH=aarch64'

linux32: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/work xerial/centos5-linux-x86 bash -c 'make clean-native native OS_NAME=Linux OS_ARCH=x86'

linux64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/work xerial/centos5-linux-x86_64 bash -c 'make clean-native native OS_NAME=Linux OS_ARCH=x86_64'

freebsd32: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir empterdose/freebsd-cross-build:9.3 sh -c 'apk add bash; apk add openjdk8; apk add perl; make clean-native native OS_NAME=FreeBSD OS_ARCH=x86 CROSS_PREFIX=i386-freebsd9-'

freebsd64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir empterdose/freebsd-cross-build:9.3 sh -c 'apk add bash; apk add openjdk8; apk add perl; make clean-native native OS_NAME=FreeBSD OS_ARCH=x86_64 CROSS_PREFIX=x86_64-freebsd9-'

freebsd-arm64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir gotson/freebsd-cross-build:aarch64-11.4 sh -c 'make clean-native native OS_NAME=FreeBSD OS_ARCH=aarch64 CROSS_PREFIX=aarch64-unknown-freebsd11-'

linux-musl32: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/work gotson/alpine-linux-x86 bash -c 'make clean-native native OS_NAME=Linux-Musl OS_ARCH=x86'

linux-musl64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/work xerial/alpine-linux-x86_64 bash -c 'make clean-native native OS_NAME=Linux-Musl OS_ARCH=x86_64'

linux-musl-arm64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-musl-arm64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=aarch64-linux-musl- OS_NAME=Linux-Musl OS_ARCH=aarch64'

linux-arm: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv5 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=armv5-unknown-linux-gnueabi- OS_NAME=Linux OS_ARCH=arm'

linux-armv6: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv6-lts -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=armv6-unknown-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armv6'

linux-armv7: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-armv7a-lts -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=arm-cortexa8_neon-linux-gnueabihf- OS_NAME=Linux OS_ARCH=armv7'

linux-arm64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-arm64-lts -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=aarch64-unknown-linux-gnu- OS_NAME=Linux OS_ARCH=aarch64'

linux-android-arm: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-android-arm -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/arm-linux-androideabi/bin/arm-linux-androideabi- OS_NAME=Linux-Android OS_ARCH=arm'

linux-android-arm64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-android-arm64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/aarch64-linux-android/bin/aarch64-linux-android- OS_NAME=Linux-Android OS_ARCH=aarch64'

linux-android-x86: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-android-x86 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/i686-linux-android/bin/i686-linux-android- OS_NAME=Linux-Android OS_ARCH=x86'

linux-android-x64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-android-x86_64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=/usr/x86_64-linux-android/bin/x86_64-linux-android- OS_NAME=Linux-Android OS_ARCH=x86_64'

linux-ppc64: $(SQLITE_UNPACKED) jni-header
	./docker/dockcross-ppc64 -a $(DOCKER_RUN_OPTS) bash -c 'make clean-native native CROSS_PREFIX=powerpc64le-unknown-linux-gnu- OS_NAME=Linux OS_ARCH=ppc64'

mac64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir -e CROSS_TRIPLE=x86_64-apple-darwin multiarch/crossbuild make clean-native native OS_NAME=Mac OS_ARCH=x86_64

mac-arm64: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir -e CROSS_TRIPLE=aarch64-apple-darwin gotson/crossbuild make clean-native native OS_NAME=Mac OS_ARCH=aarch64 CROSS_PREFIX="/usr/osxcross/bin/aarch64-apple-darwin20.4-"

# deprecated
mac32: $(SQLITE_UNPACKED) jni-header
	docker run $(DOCKER_RUN_OPTS) -v $$PWD:/workdir -e CROSS_TRIPLE=i386-apple-darwin multiarch/crossbuild make clean-native native OS_NAME=Mac OS_ARCH=x86

sparcv9:
	$(MAKE) native OS_NAME=SunOS OS_ARCH=sparcv9

mac64-signed: mac64
	$(CODESIGN) src/main/resources/org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib

mac-arm64-signed: mac-arm64
	$(CODESIGN) src/main/resources/org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib

package: native-all
	rm -rf target/dependency-maven-plugin-markers
	$(MVN) package

clean-native:
	rm -rf $(SQLITE_OUT)

clean-java:
	rm -rf $(TARGET)/*classes
	rm -rf $(TARGET)/common-lib/*
	rm -rf $(TARGET)/sqlite-jdbc-*jar

clean-tests:
	rm -rf $(TARGET)/{surefire*,testdb.jar*}

docker-linux64:
	docker build -f docker/Dockerfile.linux_x86_64 -t xerial/centos5-linux-x86_64 .

docker-linux32:
	docker build -f docker/Dockerfile.linux_x86 -t xerial/centos5-linux-x86 .

docker-linux-musl32:
	docker build -f docker/Dockerfile.alpine-linux_x86 -t gotson/alpine-linux-x86 .

docker-linux-musl64:
	docker build -f docker/Dockerfile.alpine-linux_x86_64 -t xerial/alpine-linux-x86_64 .
