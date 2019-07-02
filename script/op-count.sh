#!/bin/bash
date
# files="/mnt/opt/opendj/logs/ldap-access.audit.json*"
ops="ABANDON ADD BIND DELETE MODIFY MODIFYDN SEARCH";
status="SUCCESSFUL FAILED ";
for op in $ops;
do
    for stat in $status;
    do
      echo -n "$op-$stat,";
    done
done
echo ""
for op in $ops;
do
    for stat in $status;
    do
#     x=`grep $op $files | grep $stat | wc -l | tr -d '\n';`
      x=`grep $op $1 | grep $stat | wc -l | tr -d '\n';`
      echo -n "$x,";
    done
done
echo ""
for op in $ops;
do
    for stat in $status;
    do
      echo -n "$op $stat = ";
#       grep $op $files | grep $stat | wc -l;
      grep $op $1 | grep $stat | wc -l;
    done
done
