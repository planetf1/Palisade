#!/usr/bin/env bash
DIR1=$1
if [[ -n "$DIR1" ]]; then
   kubectl delete -f $DIR1/resource-service/k8sResourceService.yaml
else
   echo "argument error"
fi
