# Tsearch

## Introduction
A tiny searching system design for loading, transforming, indexing documents.
It also offers a group of api for searching documents and tracing the system.

## Dependences
1. Java 1.8+
2. Maven 3.0+

## Setup steps
1. git clone the project
2. cd the project forder
3. maven build (`mvn clean package`)
4. start the service (`java -jar target/tsearch-0.0.1.jar`)

## Apis
### Internal
1. health check
   1. request:
        `curl http://localhost:8080/`
   2. response:
        ```bash
        200
        ```

2. get all items in system dictionary
   1. request:
        `curl http://localhost:8080/dict`
   2. response: (can't keep the order of the items)
        ```bash
        ["上海","知乎","中华","中信证券","深圳","中华人民共和国","广州","贵州茅台","中国","人民","共和国","北京"]
        ```

### Doc load
1. file base data load
   1. request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
        -d '{
            "folderPath":"/path/to/the/docs",
            "filePrefix":"prefix_of_target_files",
            "fileSuffix":"suffix_of_target_files"
            }' \
        http://localhost:8080/loadDoc
        ```
    2. response
        ```bash
        true
        ```

### Doc search
1. 

## Enhance
1. shard the docs' index into several nodes
2. involve replica of doc index
3. involve doc index's persistence & reload
4. involve other ways of dictionary initialization
5. add system monitor thread & api