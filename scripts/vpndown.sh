#!/bin/bash
sudo ifconfig eth0 down
sudo ifconfig eth0 up
sudo route add default gw DEFAULT_GATEWAY_IP
