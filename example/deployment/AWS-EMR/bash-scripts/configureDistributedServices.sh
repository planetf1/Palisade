#!/bin/bash

# This script assumes it is running on the master node on an AWS EMR cluster
# This script will configure the Palisade Config service (which needs to be already running) so that it knows how the various Palisade services are distributed over the cluster

master_node=$1    # do we need to pass this? can we use localhost?

# identify all the yarn nodes in the cluster
declare nodes=`yarn node -list 2> /dev/null | grep internal | cut -c 4- | tr '.' '\t' | cut -f1 | tr '-' '.'`

# create the cluster address strings
for node in $nodes
do
   if [ -z "${etcd_connection_details}" ]; then
     etcd_connection_details="http://$node:2379"
     data_connection_details="http://$node:8084/data"
   else
     etcd_connection_details=${etcd_connection_details},"http://$node:2379"
     data_connection_details=${data_connection_details},"http://$node:8084/data"
   fi
done

echo "etcd_connection_details"
echo $etcd_connection_details
echo "data_connection_details"
echo $data_connection_details

# call DistributedServices class - passing it the addresses of all the Palisade services that will be running on the cluster

java -cp /home/hadoop/jars/example-model-*-shaded.jar \
    uk.gov.gchq.palisade.example.config.DistributedServices $etcd_connection_details http://localhost:8080/palisade http://localhost:8081/policy http://localhost:8082/resource http://localhost:8083/user $data_connection_details http://localhost:8085/config