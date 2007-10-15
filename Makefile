
CURRENT_DIR = $(shell cygpath -w `pwd`)

all: package 

package: win mac linux

win:
	mvn -P win clean package deploy -Dmaven.test.skip=true 
	
mac:
	mvn -P mac clean package deploy -Dmaven.test.skip=true 
	
linux:
	mvn -P linux clean package deploy -Dmaven.test.skip=true 


clean:
	mvn clean
