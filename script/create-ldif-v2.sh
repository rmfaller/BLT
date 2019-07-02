#!/bin/bash
OUPREFIX="r"
echo "dn: dc=example,dc=com"
echo "objectClass: top"
echo "objectClass: domain"
echo "dc: example"
echo ""
echo "dn: ou=People,dc=example,dc=com"
echo "objectClass: top"
echo "objectClass: organizationalUnit"
echo "ou: People"
echo ""
loopnnnn=0
while [ $loopnnnn -lt 10 ]; do
  loopnnn=0
  while [ $loopnnn -lt 10 ]; do
    loopnn=0
    while [ $loopnn -lt 10 ]; do
      loopn=0
      while [ $loopn -lt 10 ]; do
        echo "dn: ou=$OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn,dc=example,dc=com"
        echo "objectClass: top"
        echo "objectClass: organizationalUnit"
        echo "ou: $OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn"
        echo ""
        echo "dn: ou=People,ou=$OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn,dc=example,dc=com"
        echo "objectClass: top"
        echo "objectClass: organizationalUnit"
        echo "ou: People"
        echo ""
        loopooo=0
        while [ $loopooo -lt 10 ]; do
          loopoo=0
          while [ $loopoo -lt 10 ]; do
            loopo=0
            while [ $loopo -lt 10 ]; do
              echo "dn: uid=user.$loopooo.$loopoo.$loopo,ou=People,ou=$OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn,dc=example,dc=com"
              echo "objectClass: top"
              echo "objectClass: person"
              echo "objectClass: organizationalPerson"
              echo "objectClass: inetOrgPerson"
              echo "uid: user.$loopooo.$loopoo.$loopo"
              echo "cn: user.$loopooo.$loopoo.$loopo $OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn"
              echo "description: This is the description for User.$loopooo.$loopoo.$loopo of $OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn."
              echo "employeeNumber: $loopnnn.$loopnn.$loopn.$loopooo.$loopoo.$loopo"
              echo "givenName: user.$loopooo.$loopoo.$loopo"
              echo "homePhone: +1 225 216 5900"
              echo "initials: ASA"
              echo "l: Panama City"
              echo "mail: user.$loopooo.$loopoo.$loopo.$OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn@example.com"
              echo "mobile: +1 010 154 3228"
              echo "pager: +1 779 041 6341"
              echo "postalAddress: 01251 Chestnut Street,Panama City, DE  50369"
              echo "postalCode: 50369"
              echo "sn: $OUPREFIX.$loopnnnn.$loopnnn.$loopnn.$loopn"
              echo "st: DE"
              echo "street: 01251 Chestnut Street"
              echo "telephoneNumber: +1 685 622 6202"
              echo "userPassword: {SSHA512}4xxh6vRWM6n3c9tzs5dFqDVC1+6eb1wFjwWCDUX7xCvOnW6EDeEB9WbF9ff/ooZI00mRB8JrJxpb8Nfsz5rOw/pOERlaNHr3"
              echo ""
              let loopo=loopo+1
            done
            let loopoo=loopoo+1
          done
          let loopooo=loopooo+1
        done
        let loopn=loopn+1
      done
      let loopnn=loopnn+1
    done
    let loopnnn=loopnnn+1
  done
  let loopnnnn=loopnnnn+1
done
