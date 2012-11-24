How to compile a new version of SQLiteJDBC
===========================================
1. Edit the `VERSION` file
2. Edit the version number in `pom.xml` file.
3. Then, run make:
        $ make


How to submit a patch
=====================
Bitbucket Pull Request
----------------------
1. Fork this project on bitbucket
2. (do some change)
3. `hg commit -m 'what changes are made to the source'`
4. `hg push`
5. Create a pull request

Patch file
----------
1. Create a new issue on <https://bitbucket.org/xerial/sqlite-jdbc/issues>
2. Attach a patch file to issue


How to build Win64 native library
=================================
* Install cygwin with make, curl, unzip, and mingw64-x86_64-gcc-core
* (You can install MinGW64 <http://sourceforge.net/projects/mingw-w64/files/>) 

* After the installation, make sure your PATH environment variable
points to `/usr/bin` before `/bin`.

Here is the excerpt from <http://mingw-w64.sourceforge.net/>

        The mingw-w64 toolchain has been officially added to Cygwin mirrors,
        you can find the basic C toolchain as mingw64-x86_64-gcc-core. The
        languages enabled are C, Ada, C++, Fortran, Object C and Objective
        C++. There is a known caveat where calling the compiler directly as
        "/bin/x86_64-w64-mingw32-gcc" will fail, use
        "/usr/bin/x86_64-w64-mingw32-gcc" instead and make sure that your PATH
        variable has "/usr/bin" before "/bin".

* Instead, you can explicitly set the compiler:
        $ make native Windows-amd64_CC=/usr/bin/x86_64-w64-mingw32-gcc

* Then, do 
        $ make native


How to build pure-java library
==============================
* Use Mac OS X or Linux with gcc-3.x
        make purejava
* The build will fail due to the broken regex libray, so copy the non-corrupted
archive I downloaded:
        $ cp archive/regex3.8a.tar.gz target/build/nestedvm-2009-08-09/upstream/downlolad/
* then do 
        'make purejava' 


(for deployer only) How to build pure-java and native libraries
===============================================================
        make -fMakefile.package 


How to deploy to the maven repository
=====================================
    mvn deploy
    mvn deploy -Psourceforge  
(for uploading Sourceforge.jp repository, which are synchronized with the Maven
 central repository)
