#!/bin/bash
#
# Starts microservices, including the ones needed for SparqlGraph.
#
# Usage: ./startServices            to use default configuration files in src/main/resources
# Usage: ./startServices CONFIG_DIR to use configuration files in CONFIG_DIR 


# uniform JVM config for all services, for now
JVM_OPTIONS="-Xmx20G -Xincgc" 

PORT_SPARQLGRAPH_STATUS_SERVICE=12051
PORT_SPARQLGRAPH_RESULTS_SERVICE=12052
PORT_DISPATCH_SERVICE=12053
PORT_HIVE_SERVICE=12055
PORT_ORACLE_SERVICE=none
PORT_NODEGROUPSTORE_SERVICE=12056
PORT_ONTOLOGYINFO_SERVICE=12057
PORT_NODEGROUPEXECUTION_SERVICE=12058
PORT_SPARQL_QUERY_SERVICE=12050
PORT_INGESTION_SERVICE=12091
PORT_NODEGROUP_SERVICE=12059

LOCATION_ADDITIONAL_DISPATCHER_JARS=""


if [ -z "$JAVA_HOME" ]; then
        >&2 echo No JAVA_HOME
        exit
fi

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LOGS=$SEMTK/logs
echo $SEMTK

# default config file locations
CONFIG_ONTOLOGYINFO_SERVICE="$SEMTK"/ontologyInfoService/src/main/resources/ontologyinfo.properties 
CONFIG_NODEGROUPSTORE_SERVICE="$SEMTK"/nodeGroupStoreService/src/main/resources/store.properties 
CONFIG_SPARQLGRAPH_STATUS_SERVICE="$SEMTK"/sparqlGraphStatusService/src/main/resources/status.properties 
CONFIG_SPARQLGRAPH_RESULTS_SERVICE="$SEMTK"/sparqlGraphResultsService/src/main/resources/results.properties 
CONFIG_HIVE_SERVICE="$SEMTK"/hiveService/src/main/resources/hive.properties 
CONFIG_DISPATCH_SERVICE="$SEMTK"/sparqlExtDispatchService/src/main/resources/dispatch.properties
CONFIG_EXEC_SERVICE="$SEMTK"/nodeGroupExecutionService/src/main/resources/exec.properties
CONFIG_INGESTION_SERVICE="$SEMTK"/sparqlGraphIngestionService/src/main/resources/ingest.properties

# use different config files if given a config directory parameter
if [ $# -eq 1 ]; then
	CONFIG_DIR=$1
	echo USING CONFIG FILES IN "$CONFIG_DIR"
    CONFIG_ONTOLOGYINFO_SERVICE="$CONFIG_DIR"/ontologyinfo.properties
    CONFIG_NODEGROUPSTORE_SERVICE="$CONFIG_DIR"/store.properties
    CONFIG_SPARQLGRAPH_STATUS_SERVICE="$CONFIG_DIR"/status.properties
    CONFIG_SPARQLGRAPH_RESULTS_SERVICE="$CONFIG_DIR"/results.properties
    CONFIG_HIVE_SERVICE="$CONFIG_DIR"/hive.properties
    CONFIG_DISPATCH_SERVICE="$CONFIG_DIR"/dispatch.properties
    CONFIG_EXEC_SERVICE="$CONFIG_DIR"/exec.properties
    CONFIG_INGESTION_SERVICE="$CONFIG_DIR"/ingest.properties
else
	echo USING DEFAULT CONFIGS in src/main/resources/
fi


mkdir -p $LOGS

echo "=== START MICROSERVICES... ==="

# start SPARQL query service, ingestion service

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/ontologyInfoService/target/ontologyInfoService-*.jar --spring.config.location="$CONFIG_ONTOLOGYINFO_SERVICE" --server.port=$PORT_ONTOLOGYINFO_SERVICE > "$LOGS"/ontologyInfoService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupStoreService/target/nodeGroupStoreService-*.jar --spring.config.location="$CONFIG_NODEGROUPSTORE_SERVICE" --server.port=$PORT_NODEGROUPSTORE_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/nodeGroupStoreService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphStatusService/target/sparqlGraphStatusService-*.jar --spring.config.location="$CONFIG_SPARQLGRAPH_STATUS_SERVICE" --server.port=$PORT_SPARQLGRAPH_STATUS_SERVICE > "$LOGS"/sparqlGraphStatusService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphResultsService/target/sparqlGraphResultsService-*.jar --spring.config.location="$CONFIG_SPARQLGRAPH_RESULTS_SERVICE" --server.port=$PORT_SPARQLGRAPH_RESULTS_SERVICE > "$LOGS"/sparqlGraphResultsService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/hiveService/target/hiveService-*.jar --spring.config.location="$CONFIG_HIVE_SERVICE" --server.port=$PORT_HIVE_SERVICE > "$LOGS"/hiveService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -Dloader.path="$LOCATION_ADDITIONAL_DISPATCHER_JARS",${HOME}/app/lib -jar "$SEMTK"/sparqlExtDispatchService/target/sparqlExtDispatchService-*.jar --spring.config.location="$CONFIG_DISPATCH_SERVICE" --server.port=$PORT_DISPATCH_SERVICE > "$LOGS"/sparqlExtDispatchService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupExecutionService/target/nodeGroupExecutionService-*.jar --spring.config.location="$CONFIG_EXEC_SERVICE" --server.port=$PORT_NODEGROUPEXECUTION_SERVICE > "$LOGS"/storedNodegroupExecutionService.log 2>&1 &

#"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/oracleService/target/oracleService-*.jar --server.port=$PORT_ORACLE_SERVICE > "$LOGS"/oracleService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlQueryService/target/sparqlQueryService-*.jar --server.port=$PORT_SPARQL_QUERY_SERVICE > "$LOGS"/sparqlQueryService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphIngestionService/target/sparqlGraphIngestionService-*.jar --spring.config.location="$CONFIG_INGESTION_SERVICE" --server.port=$PORT_INGESTION_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/sparqlGraphIngestionService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupService/target/nodeGroupService-*.jar --server.port=$PORT_NODEGROUP_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/nodeGroupService.log 2>&1 &

echo "=== DONE ==="
