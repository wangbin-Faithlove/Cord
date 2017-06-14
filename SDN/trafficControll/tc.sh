#!/usr/bin/env bash

sudo tc qdisc add dev s1-eth5 root handle 1: htb default 15
sudo tc class add dev s1-eth5 parent 1: classid 1:1 htb rate 1000mbit ceil 1000mbit
sudo tc class add dev s1-eth5 parent 1:1 classid 1:11 htb rate 600mbit ceil 1000mbit
sudo tc class add dev s1-eth5 parent 1:1 classid 1:12 htb rate 400mbit ceil 1000mbit
sudo tc class add dev s1-eth5 parent 1:1 classid 1:13 htb rate 300mbit ceil 1000mbit
sudo tc class add dev s1-eth5 parent 1:1 classid 1:14 htb rate 200mbit ceil 1000mbit
sudo tc class add dev s1-eth5 parent 1:1 classid 1:15 htb rate 100mbit ceil 1000mbit
