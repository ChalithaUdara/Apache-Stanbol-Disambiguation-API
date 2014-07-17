#!/bin/bash
WORKSPACE=lib 

mvn install:install-file \
  -DgroupId=mpi.aida \
  -DartifactId=aida \
  -Dpackaging=jar \
  -Dversion=2.0 \
  -Dfile=${WORKSPACE}/aida.jar

mvn install:install-file \
  -DgroupId=mpi.d5 \
  -DartifactId=basics2 \
  -Dpackaging=jar \
  -Dversion=20100910 \
  -Dfile=${WORKSPACE}/basics2_20100910.jar

mvn install:install-file \
  -DgroupId=mpi.d5 \
  -DartifactId=javatools \
  -Dpackaging=jar \
  -Dversion=20130226 \
  -Dfile=${WORKSPACE}/javatools_20130226.jar

mvn install:install-file \
  -DgroupId=mpi.d5 \
  -DartifactId=DBManager \
  -Dpackaging=jar \
  -Dversion=20121219 \
  -Dfile=${WORKSPACE}/mpi-DBManager-20121219.jar

mvn install:install-file \
  -DgroupId=mpi.d5 \
  -DartifactId=TokenizerService2 \
  -Dpackaging=jar \
  -Dversion=20130410 \
  -Dfile=${WORKSPACE}/mpi-TokenizerService2-20130410.jar

mvn install:install-file \
  -DgroupId=mpi.d5 \
  -DartifactId=mpiLog \
  -Dpackaging=jar \
  -Dversion=20121123 \
  -Dfile=${WORKSPACE}/mpiLog20121123.jar

mvn install:install-file \
  -DgroupId=LBJ2 \
  -DartifactId=Library \
  -Dpackaging=jar \
  -Dversion=1.0 \
  -Dfile=${WORKSPACE}/LBJ2Library.jar

mvn install:install-file \
  -DgroupId=LBJ2 \
  -DartifactId=Chunk \
  -Dpackaging=jar \
  -Dversion=1.0 \
  -Dfile=${WORKSPACE}/LBJChunk.jar

mvn install:install-file \
  -DgroupId=LBJ2 \
  -DartifactId=POS \
  -Dpackaging=jar \
  -Dversion=1.0 \
  -Dfile=${WORKSPACE}/LBJPOS.jar


