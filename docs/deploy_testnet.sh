#!/usr/bin/env bash

mvn clean install -P test
cp target/oasisscan-test.jar /mnt/oasis-scan-test/
cd /mnt/oasis-scan-test/
sh stop.sh
sh start.sh
tail -f logs/app.log