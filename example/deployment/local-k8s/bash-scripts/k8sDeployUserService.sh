#!/usr/bin/env bash
DIR1=$1
if [[ -n "$DIR1" ]]; then
   kubectl apply -f $DIR1/user-service/k8sUserService.yaml
else
   echo "argument error"
fi
