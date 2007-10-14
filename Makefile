
all: win mac linux

win:
	mvn -P win package
	
mac:
	mvn -P mac package
	
linux:
	mvn -P linux package
	


clean:
	mvn clean
