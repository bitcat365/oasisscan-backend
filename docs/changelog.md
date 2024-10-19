The following are some changes from previous API versions (this may not be comprehensive; please refer to the actual API(<https://api.oasisscan.com/v2/swagger/>)):

| v1                                      | v2                                    | description |
| :-------------------------------------- | :------------------------------------ | :---------- |
| /chain/staking/events                   | /account/staking/events               | url         |
| /chain/staking/events/info              | /account/staking/events/info          | url         |
| event filed: type                       | event filed: kind                     | field       |
| /validator/stats                        | /validator/blocksstats                | url         |
| /chain/powerevent                       | /validator/escrowevent                | url         |
| /chain/getBlockByProposer               | /chain/proposedblocks                 | url         |
| /dashboard/network                      | /network/status                       | url         |
| /dashboard/network field: totalDelegate | /network/status field: totalDelegator | field       |

