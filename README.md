# oasisscan-api


This is the [oasisscan](https://www.oasisscan.com/) API. Previously, this API was written in Java. Due to compatibility issues, this project has been refactored into Go to better match the  [oasis-core](https://github.com/zeromicro/zero-doc/blob/main/doc/shorturl.md).

This project is developed based on [go-zero](https://github.com/zeromicro/go-zero). 

Data storage uses PostgreSQL, and no longer uses MySQL and Elasticsearch.

## Deployment

1. Install PostgreSQL


2. Initialize SQL script

   Execute the contents of docs/sql/init.sql in PostgreSQL


3. Configuration Files

   Copy and rename the following file names: `job/etc/example.yaml` and `api/etc/exmaple.yaml` like `job.yaml` and `oasisscan-api.yaml`, and then modify the database and node configuration.


4. Build Project

    ```shell
    cd job
    go build -o job
    cd api
    go build -o api
    ```
   
5. Run
    
    ```shell
   job -f job/etc/job.yaml
   api -f api/etc/oasiscan-api.yaml
   ```

## Document

[swagger](https://api.oasisscan.com/v2/swagger)

[changelog](docs/changelog.md)
