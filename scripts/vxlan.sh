#!/bin/bash
set -e 
ovs-vsctl add-br br0
ovs-vsctl add-port br0 eth0
ifconfig eth0 0 up && sudo ifconfig br0 192.168.64.136/24 up
route add default gw 192.168.64.2 br0

ovs-vsctl add-br br1
ifconfig br1 10.0.1.1/24 up

ovs-vsctl add-port br0 patch-to-br1 \
        -- set interface patch-to-br1 type=patch option:peer=patch-to-br0 \
        -- add-port br1 patch-to-br0 \
        -- set interface patch-to-br0 type=patch option:peer=patch-to-br1

ovs-vsctl  add-port br1 vxlan -- set interface vxlan type=vxlan option:remote_ip=192.168.64.131 option:key=1234
