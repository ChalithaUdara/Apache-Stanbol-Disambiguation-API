# Apache Stanbol Disambiguation REST Server based on Aidalight

In order to run this server follow the steps given below

### Requirements 
---

**server machine with at least 30GB main memory** 

## (1) Install Maven Dependancies

To install necessary maven dependancies use maven-install.sh script

```
sh maven-install.sh
```

## (2) Download disambiguation data 

To download and unzip data into data folder, run prepare-data.sh script. It will automatically
download and unzip the files to data folder

```
sh prepare-data.sh
```

## (3) Starting the Server

Use the command "mvn jetty:run-forked" to start server.

```
mvn jetty:run-forked
```
Server will be started at default port **9090**

## Testing Server
---

Server installation can be checked by using /service/example endpoint

```
curl -X GET http://localhost:9090/service/example
```

This will disambiguate the text, **With United, Beckham won the Premier League title 6 times**
