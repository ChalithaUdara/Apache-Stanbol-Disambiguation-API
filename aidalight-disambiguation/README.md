# Apache Stanbol Disambiguation Engine to use Aidalight service 

This Engine Can be used to disambiguate Yago Entities within Apache Stanbol

## Installing Engine

1) Build the bundle using mvn clean install

2) Install the bundle using Apache Stanbol bundles console

3) Now in the configuarion tab of the console, you can see the installed engine.
   Configure the property "enhancer.engine.aidalight-disambiguation.service.name" to the URL
   of the Aidalight server with port 
   E.g: http://http://localhost:9090 
 
## Using configured engine in an enhancement chain

This engine has to be used as one of the post processing engines.
You have to use this engine after one of the yago linking engines

For configuring yago linking engines refer the documentation for
https://github.com/ChalithaUdara/Stanbol-Yago-Site

