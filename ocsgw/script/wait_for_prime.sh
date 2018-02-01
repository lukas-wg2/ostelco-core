#!/bin/sh

set -e

echo "OCSGW waiting Prime to launch on 8082..."

while ! nc -z 172.16.238.4 8082; do
  sleep 0.1 # wait for 1/10 of the second before check again
done

echo "Prime launched"

# TODO call start.sh from here. For some reason, it is not working
# ./start.sh

# Start app
exec java \
    -Dfile.encoding=UTF-8 \
    -jar /ocsgw.jar

