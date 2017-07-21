#!/bin/bash
NETWORK=$1
SUBNETS=`neutron net-show $NETWORK | grep -i subnets | awk '{print $4}'`
if [[ $SUBNETS != "" ]]; then
    PORTS=`neutron port-list | grep -i $SUBNETS | awk '{print $2}'`
    for PORT in $PORTS; do
        echo "Deleting port $PORT"
        neutron port-delete $PORT
    done
fi
neutron net-delete $NETWORK
