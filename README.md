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
1. normal health check
   * request:
        `curl http://localhost:8080/`
   * response:
        ```bash
        200
        ```

2. get all items in system dictionary
   * request:
        `curl http://localhost:8080/_dict`
   * response: (can't keep the order of the items)
        ```bash
        ["上海","知乎","中华","中信证券","深圳","中华人民共和国","广州","贵州茅台","中国","人民","共和国","北京"]
        ```

3. get nodes's info stored in cache
    * request:
        `curl http://localhost:8080/_nodes`
    
    * response:
    ```bash
    {
      "_local_": {
        "nodeId": "the_node_id",
        "ip": "127.0.0.1",
        "port": 8080,
        "url": "http://127.0.0.1:8080",
        "hostname": "node_host_name",
        "upTime": 1615533870008,
        "pingRetry": 0,
        "lastPing": 0,
        "status": "GREEN"
      }
    }
    ```
   
### Doc save
1. load and index local files and save them into local node
   * request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
        -d '{
            "folderPath":"/local/path/to/the/docs",
            "filePrefix":"prefix_of_target_files",
            "fileSuffix":"suffix_of_target_files"
            }' \
        http://localhost:8080/_loadDoc
        ```
   
   * response
       ```bash
        true
        ```

2. save doc directly api, save the whole doc into system, the doc might store in the local node or
 the other node belongs to the cluster. the doc id will be automatic generated if it wasn't assigned.
   * request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
            -d '{
                    "docId": "the_doc_id",
                    "hash": "the_hash_of_doc_content",
                    "docLength": 1,
                    "content": "上海欢迎你",
                    "version": 1
                   }' \
            http://localhost:8080/_doc
        ```  

    * response:
    ```bash
    {
        "nodeId": "the_node_id_which_really_save_the_doc",
        "success" ["the_doc_ids", "which_is_stored", "successfully"],
        "failure" ["the_doc_ids", "which_is_stored", "failure"]
    }
    ```

3. save multi-docs directly api, similar with the last one
    * request:
    ```bash
    {
        "data": [
            {
                "docId": "the_doc_id",
                "hash": "the_hash_of_doc_content",
                "docLength": 1,
                "content": "上海欢迎你",
                "version": 1
            }
        ]
    }
    ```

    * response:
    ```bash
    {
        "nodeId": "the_node_id_which_really_save_the_doc",
        "success" ["the_doc_ids", "which_is_stored", "successfully"],
        "failure" ["the_doc_ids", "which_is_stored", "failure"]
    }
    ```
   
### Doc search
1. keyword search & keyword list search, only search in the current node,
    used for retrieve the docs' info in local inverted index
    * keyword search request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
                -d '{
                    "query":"中国"
                    }' \
                http://localhost:8080/_keywordSearch
        ```  
    * response:
        ```bash
        {
           "total": 1,
           "docIds": ["the_id_list_stored_in_local_node"]
        }
        ```
    
    * keyword list search request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
          -d '{
              "query":"中国"
              }' \
          http://localhost:8080/_keywordListSearch
        ```  
    * response:
        ```bash
        {
           "total": 1,
           "docIds": ["the_id_list_stored_in_local_node"]
        }
        ```

2. full text search, will analyze the query content and find out all the tokens, then try to call all nodes
(include the local node and all the other ones in the cluster) to get all the docIds
    * request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
            -d '{
                "query":"中国上海欢迎你"
                }' \
            http://localhost:8080/_search
       ```
   
   * response:
       ```bash
       {
          "total": 1,
          "docIds": ["the_id_list", "stored_in_all_nodes", "in_the_cluster"]
       }
       ```

3. get doc by the id, will return the doc info stored in cache
    * request:
    ```bash
   curl -XGET http://localhost:8080/_getDoc/the_doc_id
   ```
   
   * response:
   ```bash
   {
    "docId": "the_doc_id",
    "hash": "the_hash_of_doc_content",
    "docLength": 1,
    "content": "上海欢迎你",
    "version": 1
   }
   ```

4. get multi docs by id list, will return all the docs' info stored in the local cache against the doc ids in param
    * request:
        ```bash
        curl -XPOST -H "Content-type:application/json" \
            -d '{
                    "docIds": ["the_id_list", "stored_in_all_nodes", "in_the_cluster"]
                }' \
            http://localhost:8080/_mgetDocs
       ```
    
    * response:
    ```bash
    [
      {
        "docId": "the_doc_id",
        "hash": "the_hash_of_doc_content",
        "docLength": 1,
        "content": "上海欢迎你",
        "version": 1
       } 
    ]
    ```
    
## Enhance
1. shard the docs' index into several nodes
2. involve the replica of doc index
3. involve doc index's persistence & reload
4. involve other ways of dictionary initialization
5. add system monitor thread & api
6. involve docker deploy
7. add cached data persistence