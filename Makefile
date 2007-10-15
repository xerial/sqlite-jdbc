
CURRENT_DIR = $(shell cygpath -w `pwd`)

all: package 

package: compile win mac linux
	
compile:
	mvn compile	
	
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
