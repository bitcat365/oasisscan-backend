# 升级说明

1.先停止java程序，执行/mnt/oasis-scan下的stop.sh

2.处理es，在sense中执行命令(如不需要删除历史交易数据，可跳过此步骤)


```
删除索引：
delete /block_main_v1
delete /transaction_main_v1

建立索引：
PUT /block_main_v1
{
    "settings": {
        "index": {
            "number_of_shards": 15,
            "number_of_replicas": 0,
            "refresh_interval": "5s",
            "max_result_window": "100000000"
        }
    },
    "mappings": {
        "dynamic": "strict",
        "properties": {
            "height": {
                "type": "long",
                "ignore_malformed": true
            },
            "hash": {
                "type": "keyword"
            },
            "time": {
                "type": "date"
            },
            "txs": {
                "type": "integer",
                "ignore_malformed": true
            },
            "metadata": {
                "properties": {
                    "last_commit": {
                        "properties": {
                            "round": {
                                "type": "long",
                                "ignore_malformed": true
                            },
                            "height": {
                                "type": "long",
                                "ignore_malformed": true
                            },
                            "block_id": {
                                "properties": {
                                    "hash": {
                                        "type": "keyword"
                                    },
                                    "parts": {
                                        "type": "object",
                                        "properties": {
                                            "hash": {
                                                "type": "keyword"
                                            },
                                            "total": {
                                                "type": "long",
                                                "ignore_malformed": true
                                            }
                                        }
                                    }
                                }
                            },
                            "signatures": {
                                "properties": {
                                    "signature": {
                                        "type": "keyword"
                                    },
                                    "timestamp": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "block_id_flag": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "validator_address": {
                                        "type": "keyword"
                                    }
                                }
                            }
                        }
                    },
                    "header": {
                        "properties": {
                            "version": {
                                "properties": {
                                    "block": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "app": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    }
                                }
                            },
                            "chain_id": {
                                "type": "keyword"
                            },
                            "height": {
                                "type": "long",
                                "ignore_malformed": true,
                                "index": false
                            },
                            "time": {
                                "type": "long",
                                "ignore_malformed": true
                            },
                            "last_block_id": {
                                "properties": {
                                    "hash": {
                                        "type": "keyword"
                                    },
                                    "parts": {
                                        "type": "object",
                                        "properties": {
                                            "total": {
                                                "type": "long",
                                                "ignore_malformed": true
                                            },
                                            "hash": {
                                                "type": "keyword"
                                            }
                                        }
                                    }
                                }
                            },
                            "last_commit_hash": {
                                "type": "keyword"
                            },
                            "data_hash": {
                                "type": "keyword"
                            },
                            "validators_hash": {
                                "type": "keyword"
                            },
                            "next_validators_hash": {
                                "type": "keyword"
                            },
                            "consensus_hash": {
                                "type": "keyword"
                            },
                            "app_hash": {
                                "type": "keyword"
                            },
                            "last_results_hash": {
                                "type": "keyword"
                            },
                            "evidence_hash": {
                                "type": "keyword"
                            },
                            "proposer_address": {
                                "type": "keyword"
                            }
                        }
                    }
                }
            }
        }
    }
}

PUT /transaction_test
{
    "settings": {
        "index": {
            "number_of_shards": 30,
            "number_of_replicas": 0,
            "refresh_interval": "5s",
            "max_result_window": "100000000"
        }
    },
    "mappings": {
        "dynamic": "true",
        "properties": {
            "tx_hash": {
                "type": "keyword"
            },
            "method": {
                "type": "keyword"
            },
            "nonce": {
                "type": "long",
                "ignore_malformed": true
            },
            "height": {
                "type": "long",
                "ignore_malformed": true
            },
            "timestamp": {
                "type": "long",
                "ignore_malformed": true
            },
            "time": {
                "type": "date"
            },
            "fee": {
                "properties": {
                    "amount": {
                        "type": "long",
                        "ignore_malformed": true
                    },
                    "gas": {
                        "type": "long",
                        "ignore_malformed": true
                    }
                }
            },
            "signature": {
                "properties": {
                    "signature": {
                        "type": "keyword",
                        "index": false
                    },
                    "public_key": {
                        "type": "keyword"
                    },
                    "address": {
                        "type": "keyword"
                    }
                }
            },
            "error" : {
                "properties": {
                    "module": {
                        "type": "keyword"
                    },
                    "message": {
                        "type": "keyword",
                        "index": false
                    },
                    "code": {
                        "type": "integer",
                        "ignore_malformed": true
                    }
                }
            },
            "body": {
                "properties": {
                    "to": {
                        "type": "keyword"
                    },
                    "amount": {
                        "type": "long",
                        "ignore_malformed": true
                    },
                    "account": {
                        "type": "keyword"
                    },
                    "shares": {
                        "type": "long",
                        "ignore_malformed": true
                    },
                    "signature": {
                        "properties": {
                            "signature": {
                                "type": "keyword",
                                "index": false
                            },
                            "public_key": {
                                "type": "keyword"
                            }
                        }
                    },
                    "signatures": {
                        "properties": {
                            "signature": {
                                "type": "keyword",
                                "index": false
                            },
                            "public_key": {
                                "type": "keyword"
                            }
                        }
                    },
                    "amendment": {
                        "properties": {
                            "rates": {
                                "properties": {
                                    "start": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "rate": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    }
                                }
                            },
                            "bounds": {
                                "properties": {
                                    "start": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "rate_min": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "rate_max": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    }
                                }
                            }
                        }
                    }
                }
            },
            "node": {
                "properties": {
                    "roles": {
                        "type": "long",
                        "ignore_malformed": true
                    },
                    "expiration": {
                        "type": "long",
                        "ignore_malformed": true
                    },
                    "id": {
                        "type": "keyword"
                    },
                    "entity_id": {
                        "type": "keyword"
                    },
                    "nodes": {
                        "type": "keyword"
                    },
                    "v": {
                        "type": "long",
                        "ignore_malformed": true,
                        "index": false
                    },
                    "tls" : {
                        "properties" : {
                            "pub_key" : {
                                "type": "keyword"
                            },
                            "next_pub_key" : {
                                "type": "keyword"
                            },
                            "addresses" : {
                                "properties": {
                                    "next_pub_key" : {
                                        "type": "keyword"
                                    },
                                    "address": {
                                        "properties": {
                                            "IP": {
                                                "type": "keyword"
                                            },
                                            "Port": {
                                                "type": "keyword"
                                            },
                                            "Zone": {
                                                "type": "keyword"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "p2p": {
                        "properties": {
                            "id": {
                                "type": "keyword"
                            },
                            "addresses": {
                                "properties": {
                                    "IP": {
                                        "type": "keyword"
                                    },
                                    "Port": {
                                        "type": "keyword"
                                    },
                                    "Zone": {
                                        "type": "keyword"
                                    }
                                }
                            }
                        }
                    },
                    "consensus": {
                        "properties": {
                            "id": {
                                "type": "keyword"
                            },
                            "addresses": {
                                "properties": {
                                    "id": {
                                        "type": "keyword"
                                    },
                                    "address": {
                                        "properties": {
                                            "IP": {
                                                "type": "keyword"
                                            },
                                            "Port": {
                                                "type": "keyword"
                                            },
                                            "Zone": {
                                                "type": "keyword"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "runtimes": {
                        "properties": {
                            "id": {
                                "type": "keyword"
                            },
                            "extra_info": {
                                "type": "keyword",
                                "index": false
                            },
                            "version": {
                                "properties": {
                                    "Major": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "Minor": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    },
                                    "Patch": {
                                        "type": "long",
                                        "ignore_malformed": true
                                    }
                                }
                            },
                            "capabilities": {
                                "properties": {
                                    "tee": {
                                        "properties": {
                                            "hardware": {
                                                "type": "integer",
                                                "ignore_malformed": true,
                                                "index": false
                                            },
                                            "rak": {
                                                "type": "keyword",
                                                "index": false
                                            },
                                            "attestation": {
                                                "type": "keyword",
                                                "index": false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

创建索引别名：
PUT /block_main_v1/_alias/block
PUT /transaction_main_v1/_alias/transaction
```
3.处理mysql（除system_property以及validator_info以外的所有表记录清空）
```
truncate account;
truncate debonding;
truncate delegator;
truncate escrow_stats;
truncate validator_consensus;
```

4.启动java程序，执行/mnt/oasis-scan下的start.sh
