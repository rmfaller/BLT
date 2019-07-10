#!/bin/bash
ts=`date +"%m-%d-%Y-%T"`
echo $ts > $HOME/tmp/$ts-connections.out
echo "+++ netstat +++" >> $HOME/tmp/$ts-connections.out
netstat -anp | grep ESTABLISHED | grep 1389 >> $HOME/tmp/$ts-connections.out
echo "--- lsof ---" >> $HOME/tmp/$ts-connections.out
lsof -i  >> $HOME/tmp/$ts-connections.out
echo "~~~ ss ~~~" >> $HOME/tmp/$ts-connections.out
ss -s  >> $HOME/tmp/$ts-connections.out