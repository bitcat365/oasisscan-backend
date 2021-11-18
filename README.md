# oasisscan-api

* [validator-stats](#validator-stats)
* [validator-list](#validator-list)
* [validator-info](#validator-info)
* [network](#network)
* [transactions](#transactions)
* [blocks](#blocks)
* [block-info](#block-info)
* [transaction-info](#transaction-info)
* [methods](#methods)
* [transaction-history](#transaction-history)
* [blockByProposer](#blockByProposer)
* [entity-info](#entity-info)
* [powerEvent](#powerEvent)
* [delegators](#delegators)
* [escrow-stats](#escrow-stats)
* [account-list](#account-list)
* [account-info](#account-info)
* [search](#search)
* [account-delegations](#account-delegations)
* [account-debonding](#account-debonding)
* [runtime-round-list](#runtime-round-list)
* [runtime-round-info](#runtime-round-info)
* [runtime-list](#runtime-list)
* [runtime-stats](#runtime-stats)
* [runtime-transactions](#runtime-transactions)
* [runtime-transaction-info](#runtime-transaction-info)

### validator stats

```
// Request
GET http://localhost:8181/validator/stats?address=oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p

// Response
{
    "code": 0,
    "data": {
        "signs": [
            {
                "height": 483023,
                "block": false
            },
            {
                "height": 483022,
                "block": false
            }
        ],
        "proposals": [
            {
                "height": 483023,
                "block": false
            },
            {
                "height": 483022,
                "block": true
            }
        ]
    }
}
```

### validator list

```
// Request
GET http://localhost:8181/validator/list

```

Params

|  param   | description  |
|  ----  | ----  |
| orderBy  | sort field(escrow,escrowChange24,delegators,commission,uptime),default:escrow |
| sort  | asc,desc, default:desc |
| page  | default:1 |
| pageSize  | default:300 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "rank": 1,
                "entityId": "6JbJjfiQod2O2ONGJ5y4TdqjrzKbZz0xjV92L3BcotQ=",
                "entityAddress": "oasis1qrx376dmwuckmruzn9vq64n49clw72lywctvxdf4",
                "nodeId": "t/jy9WHxQdqHT4flEUFq5voSRYBSMSGDV9oJEVMrtRI=",
                "nodeAddress": "oasis1qpymp2jl9ntn85q87ap38207lzgpanynugd6jyhf",
                "name": "oasis1qr...wctvxdf4",
                "icon": null,
                "website": null,
                "description": null,
                "escrow": "1554050.76",
                "escrowChange24": "372.93",
                "escrowPercent": 0.13,
                "balance": "0",
                "totalShares": "1550000",
                "signs": 175,
                "proposals": 28,
                "nonce": 0,
                "score": 231,
                "delegators": 0,
                "nodes": null,
                "uptime": "18%",
                "active": true,
                "commission": 0.0,
                "rates": null,
                "bounds": null,
                "escrowSharesStatus": null,
                "escrowAmountStatus": null,
                "status": true
            }
        ],
        "active": 1,
        "inactive": 0,
        "delegators": 0
    }
}
```

### validator-info

```
// Request
GET http://localhost:8181/validator/info?address=oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p

// Response
{
    "code": 0,
    "data": {
        "rank": 11,
        "entityId": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
        "entityAddress": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
        "nodeId": "3IbxhcOu3j2o/0Mk5V3qXOfasEEm42pJpzuWAf8SREg=",
        "nodeAddress": "oasis1qpc34h9cm0wrvkrepvzhcz0mpmjmr4dv6g24yayr",
        "name": "Bit Cat",
        "icon": "https://s3.amazonaws.com/keybase_processed_uploads/e6d2c9be95cde136dcf0ade7238f1705_360_360.jpg",
        "website": "https://www.bitcat365.com/",
        "twitter": "https://twitter.com/BitCat365",
        "keybase": "https://keybase.io/bitcat365",
        "email": "bitcat365.com@gmail.com",
        "description": null,
        "escrow": "190962.83",
        "escrowChange24": "45.83",
        "escrowPercent": 0.0,
        "balance": "1",
        "totalShares": "190023.32",
        "signs": 303568,
        "proposals": 4934,
        "nonce": 13,
        "score": 313436,
        "delegators": 1,
        "nodes": null,
        "uptime": "100%",
        "active": true,
        "commission": 0.1,
        "bound": {
            "start": 400,
            "min": 0.0,
            "max": 1.0
        },
        "rates": [
            {
                "start": 420,
                "rate": 0.1
            }
        ],
        "bounds": [
            {
                "start": 400,
                "min": 0.0,
                "max": 1.0
            }
        ],
        "escrowSharesStatus": {
            "self": "190023.32",
            "other": "0.0",
            "total": "190023.32"
        },
        "escrowAmountStatus": {
            "self": "190962.83",
            "other": "0.0",
            "total": "190962.83"
        },
        "status": true
    }
}
```

### network

```
// Request
GET http://localhost:8181/dashboard/network

// Response
{
    "code": 0,
    "data": {
        "curHeight": 262360,
        "curEpoch": 8652,
        "totalTxs": 192,
        "totalEscrow": null,
        "activeValidator": 0,
        "totalDelegate": 0
    }
}
```

### transactions

```
// Request
GET http://localhost:8181/chain/transactions

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:10 |
| page  | option,default:1 |
| height  | option |
| address  | option |
| method  | option |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "txHash": "bc5fbea3e534e046e28ca104cc7988691005b57a308c99ee36fbdda212967744",
                "height": 748,
                "method": "registry.RegisterNode",
                "fee": "0",
                "timestamp": 1589221913,
                "time": 1553609,
                "success": true
            }
        ],
        "page": 1,
        "size": 10,
        "maxPage": 1000,
        "totalSize": 10000
    }
}
```

### blocks

```
// Request
GET http://localhost:8181/chain/blocks

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:10 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
             {
                 "height": 203850,
                 "hash": "9a75550ec5308f6cb4208bbcabcbc0d32cd92bf301ede5f7f957ca05eb058496",
                 "proposer": "clGHKEfX8nHkge957UTPdwuLlyM=",
                 "entityId": "6JbJjfiQod2O2ONGJ5y4TdqjrzKbZz0xjV92L3BcotQ=",
                 "entityAddress": "oasis1qrx376dmwuckmruzn9vq64n49clw72lywctvxdf4",
                 "name": "Staking Fund",
                 "txs": 4,
                 "timestamp": 1593693834,
                 "time": 15
             },
             {
                 "height": 203849,
                 "hash": "bc34dea2c3317365ef409a0e982e6e8fed79dc2420c7f712188730309932c3b4",
                 "proposer": "3NQgDMD7HNeNXa2S4tFqlgn8hdk=",
                 "entityId": "Xq2d4D43YmBdUAOd3q6A0n1kaHUY766RE24xznk1Sgc=",
                 "entityAddress": "oasis1qrem4skslfwn3zcutpgxyd7664naqyplsy2ymrv6",
                 "name": "Simply Staking",
                 "txs": 5,
                 "timestamp": 1593693828,
                 "time": 21
             }
        ],
        "page": 1,
        "size": 10,
        "maxPage": 1000,
        "totalSize": 10000
    }
}
```

### block-info

```
// Request
GET http://localhost:8181/chain/block/{height}

```

Params

|  param   | description  |
|  ----  | ----  |
| height  | height |

```

// Response
{
    "code": 0,
    "data": {
        "height": 2,
        "epoch": 0,
        "timestamp": 1589216250,
        "time": 1619412,
        "hash": "irGcRXNjYnlo03zZE0FesdYqVf8elhac7gyS3Gp9ZZM=",
        "txs": 32,
        "proposer": "MwHY8KdolvZZ0JbT1n7/gVO8Iu8="
    }
}
```

### transaction-info

```
// Request
GET http://localhost:8181/chain/transaction/{hash}

```

Params

|  param   | description  |
|  ----  | ----  |
| hash  | tx hash |

```

// Response
{
    "code": 0,
    "data": {
        "txHash": "182815b0eac3c6773d19d1bd48c6ec98f323167a2e5873182df413d7945e6137",
        "timestamp": 1589348475,
        "time": 1510517,
        "height": 22218,
        "fee": "0.000000001",
        "nonce": 0,
        "method": "staking.AddEscrow",
        "from": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
        "to": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
        "amount": "799",
        "raw": "{\"tx_hash\":\"182815b0eac3c6773d19d1bd48c6ec98f323167a2e5873182df413d7945e6137\",\"method\":\"staking.AddEscrow\",\"fee\":{\"gas\":1000,\"amount\":\"1\"},\"body\":{\"escrow_account\":\"CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=\",\"escrow_tokens\":\"799000000000\"},\"nonce\":10,\"signature\":{\"signature\":\"pgbgv0fjDBiIYV/mqIGlbIrzH4o7FXdxJVaWwv14VnIDnWFnYuuVUi6i0PWCzu8OoG3ZGanY1fmGZAyp+m+GCQ==\",\"public_key\":\"CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=\"},\"height\":22218,\"timestamp\":1589348475}",
        "status": false,
        "errorMessage": "registry: node expired"
    }
}
```

### methods

```
// Request
GET http://localhost:8181/chain/methods

// Response
{
    "code": 0,
    "data": {
        "list": [
            "registry.RegisterNode",
            "staking.AddEscrow",
            "staking.ReclaimEscrow",
            "staking.Transfer",
            "registry.RegisterEntity",
            "staking.AmendCommissionSchedule"
        ]
    }
}
```

### transaction-history

```
// Request
GET http://localhost:8181/chain/transactionhistory

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "key": "1589155200",
                "value": 5548
            },
            {
                "key": "1589241600",
                "value": 11035
            },
            {
                "key": "1589328000",
                "value": 10238
            },
            {
                "key": "1589414400",
                "value": 12085
            },
            {
                "key": "1589500800",
                "value": 12388
            },
            {
                "key": "1589587200",
                "value": 12749
            },
            {
                "key": "1589673600",
                "value": 12739
            },
            {
                "key": "1589760000",
                "value": 13559
            },
            {
                "key": "1589846400",
                "value": 13316
            },
            {
                "key": "1589932800",
                "value": 15083
            },
            {
                "key": "1590019200",
                "value": 9081
            }
        ]
    }
}
```

### blockByProposer

```
// Request
GET http://localhost:8181/chain/getBlockByProposer

```

Params

|  param   | description  |
|  ----  | ----  |
| address  | validator address |
| size  | option,default:10 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "height": 203332,
                "hash": "7d8b7a65dd670cee42f856ac049ae7b3ccab34825075181056b03a21647d24a3",
                "proposer": "2G4wTuMDkbAQvWxseX5b76Ap6q0=",
                "entityId": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
                "entityAddress": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
                "name": "Bit Cat",
                "txs": 8,
                "timestamp": 1593690774,
                "time": 39
            }
        ],
        "page": 1,
        "size": 10,
        "maxPage": 1000,
        "totalSize": 10000
    }
}
```

### entity-info

```
// Request
GET http://localhost:8181/chain/account/info/{account}

// Response
{
    "code": 0,
    "data": {
        "entityId": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
        "totalBalance": "21548.05009699",
        "available": "0.895929958",
        "escrow": "21547.154167032",
        "debonding": "0"
    }
}
```

### power event

```
// Request
GET http://localhost:8181/chain/powerevent

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:5 |
| page  | option,default:1 |
| address  | option |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "txHash": "45714ad56cb8af4d1caa911ad6fee46e30c3336fede33b9cf1d06320eeb5ccc5",
                "height": 466786,
                "method": "staking.ReclaimEscrow",
                "fee": "0.000002",
                "amount": "0.39",
                "shares": "5000.0",
                "add": false,
                "timestamp": 1591983773,
                "time": 72346
            },
            {
                "txHash": "46085a1c6808e0d1d96372c0859607810bf1d3dbaedd068f3a5ea1883e8e3b37",
                "height": 466670,
                "method": "staking.AddEscrow",
                "fee": "0.000002",
                "amount": "0.0",
                "shares": "0.0",
                "add": true,
                "timestamp": 1591983083,
                "time": 73037
            },
            {
                "txHash": "4e9b55e31371900f4c3e1efe5e9943825546b1ee6ca076746b5eca0636b35d49",
                "height": 466600,
                "method": "staking.ReclaimEscrow",
                "fee": "0.000002",
                "amount": "0.0",
                "shares": "0.0",
                "add": false,
                "timestamp": 1591982668,
                "time": 73453
            },
            {
                "txHash": "182815b0eac3c6773d19d1bd48c6ec98f323167a2e5873182df413d7945e6137",
                "height": 22218,
                "method": "staking.AddEscrow",
                "fee": "0.000000001",
                "amount": "799.0",
                "shares": "11131.93",
                "add": true,
                "timestamp": 1589348475,
                "time": 2707647
            },
            {
                "txHash": "2f4d80b41c9452e97d3d9cf7b36551c750716519fabba6d4c2cabc88a0ccb68d",
                "height": 5188,
                "method": "staking.ReclaimEscrow",
                "fee": "0.000002",
                "amount": "0.05",
                "shares": "360.0",
                "add": false,
                "timestamp": 1589248409,
                "time": 2807715
            }
        ],
        "page": 1,
        "size": 5,
        "maxPage": 6,
        "totalSize": 28
    }
}
```

### delegators

```
// Request
GET http://localhost:8181/validator/delegators

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:5 |
| page  | option,default:1 |
| address  | validator address |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "address": "CVzqFIADD2Ed0khGBNf4Rvh7vSNtrL1ULTkWYQszDpc=",
                "amount": "12167.93",
                "shares": "6523.7",
                "percent": 0.9135,
                "self": true
            },
            {
                "address": "dXuwlm7j9ZEwO+gi5mrnVZOPL6Z/Ru5VUXWel+gwQ9U=",
                "amount": "1000.32",
                "shares": "536.31",
                "percent": 0.0751,
                "self": false
            },
            {
                "address": "5s7OfLEjIrePZ35UnkwrPQ7YVyzd5HAiddbWZKYLo5w=",
                "amount": "118.14",
                "shares": "63.34",
                "percent": 0.0089,
                "self": false
            },
            {
                "address": "wgMBa/9qR/XwnlgYXAXvqHMXT+LvkOX0JcQc0CcW4HQ=",
                "amount": "33.01",
                "shares": "17.7",
                "percent": 0.0025,
                "self": false
            }
        ],
        "page": 1,
        "size": 5,
        "maxPage": 1,
        "totalSize": 4
    }
}
```

### escrow-stats

```
// Request
GET http://localhost:8181/validator/escrowstats

```

Params

|  param   | description  |
|  ----  | ----  |
| address  | validator address |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "timestamp": 1594080000000,
                "escrow": "190854.01"
            },
            {
                "timestamp": 1593993600000,
                "escrow": "190806.3"
            }
        ]
    }
}
```

### account-list

```
// Request
GET http://localhost:8181/chain/account/list

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:100 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "rank": 1,
                "address": "oasis1qzs9gq9qsuju5v78th5v05tnwdxfljy3vsfc3cjy",
                "available": "999555700",
                "escrow": "439157.38",
                "debonding": "0",
                "total": "999994857.38"
            },
            {
                "rank": 2,
                "address": "oasis1qpy9d8kun7f2r2e5z2u3e0r4fxfwtflhvvk7qa8l",
                "available": "500000000",
                "escrow": "0",
                "debonding": "0",
                "total": "500000000"
            }
        ],
        "page": 1,
        "size": 100,
        "maxPage": 5,
        "totalSize": 481
    }
}
```

### account-info

```
// Request
GET http://localhost:8181/chain/account/info/{address}

// Response
{
    "code": 0,
    "data": {
        "rank": 0,
        "address": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
        "available": "0.0009",
        "escrow": "190043.6452",
        "debonding": "4.0211",
        "total": "190047.6672",
        "nonce": 20
        "allowances": [
            {
                "address": "oasis1qpgj8l300z3xcmwtwcue623zv6h9g7fftugx0q94",
                "amount": "900.0000"
            }
        ]
    }
}
```

### search

```
// Request
GET http://localhost:8181/chain/search

```

Params

|  param   | description  |
|  ----  | ----  |
| key  | key |

```

// Response
{
    "code": 0,
    "data": {
        "key": "oasis1qpc34h9cm0wrvkrepvzhcz0mpmjmr4dv6g24yayr",
        "type": "validator",
        "result": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p"
    }
}
```

note：result is the key parameter required to jump to the target type page

|  type   | description  |
|  ----  | ----  |
| none  | no result |
| block  | block |
| transaction  | transaction |
| validator  | validator |
| account  | account |
| runtime-transaction  | runtime-transaction |

### account-delegations

```
// Request
GET http://localhost:8181/chain/account/delegations

```

Params

|  param   | description  |
|  ----  | ----  |
| address  | account address |
| all  | option,include non-validators,default:false |
| size  | option,default:5 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "validatorAddress": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
                "validatorName": "Bit Cat",
                "shares": "190062.46",
                "amount": "191392.77"
            },
            {
                "validatorAddress": null,
                "validatorName": null,
                "entityAddress": "oasis1qrwdwxutpyr9d2m84zh55rzcf99aw0hkts7myvv9",
                "shares": "10000000",
                "amount": "10000000"
            }
        ],
        "page": 1,
        "size": 5,
        "maxPage": 1,
        "totalSize": 1
    }
}
```

### account-debonding

```
// Request
GET http://localhost:8181/chain/account/debonding

```

Params

|  param   | description  |
|  ----  | ----  |
| address  | account address |
| size  | option,default:5 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "validatorAddress": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
                "validatorName": "Bit Cat",
                "shares": "2.01",
                "debondEnd": 847
            },
            {
                "validatorAddress": "oasis1qzskk72k92y4duc47lqcsxhza6fahlqtlqyesw0p",
                "validatorName": "Bit Cat",
                "shares": "2.01",
                "debondEnd": 912
            }
        ],
        "page": 1,
        "size": 5,
        "maxPage": 1,
        "totalSize": 2
    }
}
```

### runtime-round-list

```
// Request
GET http://localhost:8181/runtime/round/list

```

Params

|  param   | description  |
|  ----  | ----  |
| id  | runtime id |
| size  | option,default:5 |
| page  | option,default:1 |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "version": 0,
                "runtimeId": "00000000000000000000000000000000000000000000000072c8215e60d5bca7",
                "runtimeName": "Emerald",
                "round": 16,
                "timestamp": 1629225107,
                "header_type": 1,
                "previous_hash": "oSGJn5+baKA+rkwJfkhshGNoJq2zWR/8uMKthOdQxo0=",
                "io_root": "EJjDyErNa+22fvlYJ+NNohpZY7uW6ErNyoTs/zBcTHQ=",
                "state_root": "M3TdOyhPZ+Za+l3oBigYrJmlYArDF8Vpsj4pu/8YyTI=",
                "messages_hash": "xnK40e9W7Sirh8NiLFEUBpvdOte4+XN0mNDAHs7wlno="
            }
        ],
        "page": 1,
        "size": 10,
        "maxPage": 2,
        "totalSize": 17
    }
}
```

### runtime-round-info

```
// Request
GET http://localhost:8181/runtime/round/info

```

Params

|  param   | description  |
|  ----  | ----  |
| id  | runtime id |
| round  | runtime round |

```

// Response
{
    "code": 0,
    "data": {
        "version": 0,
        "runtimeId": "00000000000000000000000000000000000000000000000072c8215e60d5bca7",
        "runtimeName": "Emerald",
        "round": 3,
        "timestamp": 1629224541,
        "header_type": 1,
        "previous_hash": "cRuVoUZ+Xivqtli6pdfIbXl8ZX0ZqX+mZ4WbLSwQFaQ=",
        "io_root": "xnK40e9W7Sirh8NiLFEUBpvdOte4+XN0mNDAHs7wlno=",
        "state_root": "PbxPXDxWgtm7gVlztThWxSICJhlDWp/z75RPKCJMLoI=",
        "messages_hash": "xnK40e9W7Sirh8NiLFEUBpvdOte4+XN0mNDAHs7wlno=",
        "next": true
    }
}
```

### runtime-list

```
// Request
GET http://localhost:8181/runtime/list

```

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "name": null,
                "runtimeId": "AAAAAAAAAAAAAAAAAAAAAAAAcGFyY2Vsc3RhZwAAAAI="
            },
            {
                "name": null,
                "runtimeId": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            }
        ]
    }
}
```

### runtime-stats

```
// Request
GET http://localhost:8181/runtime/stats

```

Params

|  param   | description  |
|  ----  | ----  |
| id  | runtime id |
| sort  | integer,default:0 |

Sort

|  field   | index  |
|  ----  | ----  |
|ELECTED|0|
|PRIMARY|1|
|BACKUP|2|
|PROPOSER|3|
|PRIMARY_INVOKED|4|
|PRIMARY_GOOD_COMMIT|5|
|PRIM_BAD_COMMMIT|6|
|BCKP_INVOKED|7|
|BCKP_GOOD_COMMIT|8|
|BCKP_BAD_COMMIT|9|
|PRIMARY_MISSED|10|
|BCKP_MISSED|11|
|PROPOSER_MISSED|12|
|PROPOSED_TIMEOUT|13|

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "entityId": "qg5I1u74cSaDfg9GA8SAvTxronahNfCXfj1tIjeV6hQ=",
                "name": null,
                "address": "oasis1qrtle8dp3me9cac3mekxcnyq9qhsn6r67q32sazp",
                "validator": false,
                "icon": null,
                "status": true,
                "stats": {
                    "elected": 14,
                    "primary": 14,
                    "backup": 0,
                    "proposer": 14,
                    "primary_invoked": 13,
                    "primary_good_commit": 13,
                    "prim_bad_commmit": 0,
                    "bckp_invoked": 0,
                    "bckp_good_commit": 0,
                    "bckp_bad_commit": 0,
                    "primary_missed": 0,
                    "bckp_missed": 0,
                    "proposer_missed": 0,
                    "proposed_timeout": 0
                }
            }
        ]
    }
}
```

### runtime-transactions

```
// Request
GET http://localhost:8181/runtime/transaction/list

```

Params

|  param   | description  |
|  ----  | ----  |
| size  | option,default:10 |
| page  | option,default:1 |
| id  | runtime id |
| round  | option,runtime round |

```

// Response
{
    "code": 0,
    "data": {
        "list": [
            {
                "runtimeId": "00000000000000000000000000000000000000000000000072c8215e60d5bca7",
                "txHash": "0x4bea2e805d3a3feb62bcdb4fb04b3268141abc698c143f80b37cf54a29149838",
                "round": 1202,
                "result": true,
                "timestamp": 1636049829,
                "type": "evm"
            }
        ],
        "page": 1,
        "size": 10,
        "maxPage": 28,
        "totalSize": 278
    }
}
```

### runtime-transaction-info

```
// Request
GET http://localhost:8181/runtime/transaction/info

```

Params

|  param   | description  |
|  ----  | ----  |
| id  | runtime id |
| hash  | tx hash |

```

// Response
consensus transaction
{
    "code": 0,
    "data": {
        "runtimeId": "00000000000000000000000000000000000000000000000072c8215e60d5bca7",
        "runtimeName": "Emerald",
        "txHash": "55354c5be461418cf950d6a2830f5b27d7a76c38d1cb263922603480af29fe79",
        "round": 1764,
        "result": true,
        "message": "null",
        "timestamp": 1636646196,
        "type": "consensus",
        "ctx": {
            "method": "consensus.Deposit",
            "from": "oasis1qr9ugwvu337v9hcwuc982ccphd0y9yqd4y4gwkgz",
            "to": "oasis1qz9dwcj9vn9q5w47cenw6dhsqmn49fdapgn4nzjq",
            "amount": "2.123",
            "nonce": 0
        },
        "etx": null
    }
}

evm transaction
{
    "code": 0,
    "data": {
        "runtimeId": "00000000000000000000000000000000000000000000000072c8215e60d5bca7",
        "runtimeName": "Emerald",
        "txHash": "0x83e613a6219ede8bf8f89c7c9fbc7f864c9097554b07cb5c4a94af4bf0095b53",
        "round": 1754,
        "result": true,
        "message": "AAAAAAAAAAAAAAAApYTOzdAj85OqqBJ3srgcqL45UZc=",
        "timestamp": 1636644459,
        "type": "evm",
        "ctx": null,
        "etx": {
            "from": "0x8ac3195aeca398aac7882520dd19d3c7c5e69e46",
            "to": "0x8c421cd16d55248cce8e36d1d68682cf1373ee78",
            "nonce": 61,
            "gasPrice": 0,
            "gasLimit": 2354836,
            "data": "c9c65396000000000000000000000000e6c87c360c24efc6fef4dcefed5607b0adacf936000000000000000000000000ca8a7b55a04a9fde7ae7bf128384fa330f81a19c",
            "value": "0"
        }
    }
}
```
