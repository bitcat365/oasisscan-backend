# Deploy oasisscan-backend

## Requirement
Please install [oasis_api_server](https://github.com/bitcat365/oasis_api_server) first.You can choose to use the mainnet branch or the testnet branch to correspond to different oasis node networks.

Please install elasticsearch and mysql to save blockchain and validators data.

- [elasticsearch(version>=7.0)](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- mysql

You can configure the elasticsearch index through the [mapping file](mapping/),and initialize mysql through the [init.sql](docs/init.sql)

## Configuring oasisscan-backend
There are three configuration files for three different environments. [application-prod](../src/main/resources/application-prod.yaml) for mainnet, [application-test](../src/main/resources/application-test.yaml) for testnet.

Please modify the configuration of **elasticserach, mysql and oasis**.


## Running oasisscan-backend
Modify the path configuration in the [deploy file](deploy_mainnet.sh) and execute.
```
mvn clean install -P prod
cp target/oasisscan-prod.jar /mnt/oasis-scan/
cd /mnt/oasis-scan/
sh stop.sh
sh start.sh
tail -f logs/app.hz-oasis-scan.log
```

start.sh
```
/mnt/jdk1.8.0_241/bin/java -server -Xms1g -Xmx1g -Dserver.port=8181 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod -Dsun.net.inetaddr.ttl=60 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -XX:+PrintGCDateStamps -verbose:gc -XX:+PrintGCDetails -Xloggc:logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/oom.hprof -jar /mnt/oasis-scan/oasisscan-prod.jar
```
stop.sh
```
ps -ef |  grep oasisscan-prod.jar  | awk '{print$2}' | xargs kill
```