#!/usr/bin/env bash

mvn clean install -P prod
cp target/oasisscan-prod.jar /mnt/oasis-scan/
cd /mnt/oasis-scan/
sh stop.sh
sh start.sh
tail -f logs/app.log