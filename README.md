Required to build: java1.8/maven
Required to run: java1.8

Steps to launch:
1. Define type of connection (proxy/direct) and list of proxies in application.properties file. 
   Those proxies will be randomly selected for each request. Using several proxies looks to be the most reliable way to avoid throttling/anti-crawling restrictions. 
2. Build with maven: "mvn clean install"
3. Run as follows: "java -Dfile.encoding=UTF-8 -jar crawler-1.0-SNAPSHOT.jar"

 