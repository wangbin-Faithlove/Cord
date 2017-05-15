#!/bin/bash
# 使用前安装pptp-linux工具
set -e 
python ~/login.py
sudo pptpsetup --create jiangge02 --server us01.blockcn.net --username *** --password **** --encrypt --start
sudo ip route del default
sudo ip route add default dev ppp0
