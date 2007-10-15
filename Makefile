
CURRENT_DIR = $(shell cygpath -w `pwd`)

all: package 

package: win mac linux
	
win:
	mvn -P win package
	
mac:
	mvn -P mac package
	
linux:
	mvn -P linux package
	

deploy: 
	mvn -P win deploy
	mvn -P mac deploy
	mvn -P linux deploy

clean:
	mvn clean
