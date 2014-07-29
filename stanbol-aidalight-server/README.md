# Apache Stanbol Disambiguation REST Server based on Aidalight

In order to run this server follow the steps given below 

## (1) Install Maven Dependancies

To install necessary maven dependancies to use the server run maven-install.sh script

## (2) Download disambiguation data 

To download and unzip data into data folder, run prepare-data.sh script. It will automatically
download and unzip the files to data folder

## (3) Starting the Server

Use the command "mvn jetty:run-forked" to start server
