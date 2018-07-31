#!/bin/bash
dirn=`dirname $0`
TANGERINE=`cd -P $dirn/../..  ; echo $PWD`
DEMO=`cd -P $dirn; echo $PWD`

if [ "$1" = "build" ]; then
  cd $TANGERINE
  rm ./client/lib/*jar
  mvn clean install
  mvn dependency:copy-dependencies

  cd $DEMO/apps/netowl_app
  rm ./lib/*jar
  mvn clean install
  mvn dependency:copy-dependencies
  
  cd $DEMO
  mkdir -p lib
fi

cd $DEMO

# This relies on having already run "mvn install" at $TANGERINE
# 
cp $TANGERINE/client/target/client.jar $DEMO
cp $TANGERINE/client/lib/*jar $DEMO/lib
cp $DEMO/apps/netowl_app/target/netowlapp.jar $DEMO
cp $DEMO/apps/netowl_app/lib/*jar $DEMO/lib

# In your app environment, export PYTHONPATH=$DEMO/piplib
#
pip install --upgrade --target $DEMO/piplib requests  
