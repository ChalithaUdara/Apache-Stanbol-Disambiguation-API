#!/bin/bash
# This script will download the data file needed to run the server

WORKSPACE=data

echo "Downloading Data............"
curl -o ${WORKSPACE}/bned.zip http://download.mpi-inf.mpg.de/d5/aida/bned.zip
echo "Download Completed............"
echo "Unziping Data............."
unzip ${WORKSPACE}/bned.zip
echo "Completed............"
