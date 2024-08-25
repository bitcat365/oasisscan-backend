// Code generated by goctl. DO NOT EDIT.
package types

type AccountAllowance struct {
	Address string `path:"address"`
	Amount  string `path:"amount"`
}

type AccountDebondingInfo struct {
	ValidatorAddress string `json:"validatorAddress"`
	ValidatorName    string `json:"validatorName"`
	Icon             string `json:"icon"`
	Shares           string `json:"shares"`
	DebondEnd        int64  `json:"debondEnd"`
	EpochLeft        int64  `json:"epochLeft"`
}

type AccountDebondingRequest struct {
	Address string `form:"address"`
	Page    int64  `form:"page,default=1"`
	Size    int64  `form:"size,default=5"`
}

type AccountDebondingResponse struct {
	List []*AccountDebondingInfo `json:"list"`
	Page
}

type AccountDelegationsInfo struct {
	ValidatorAddress string `json:"validatorAddress"`
	ValidatorName    string `json:"validatorName"`
	Icon             string `json:"icon"`
	EntityAddress    string `json:"entityAddress"`
	Shares           string `json:"shares"`
	Amount           string `json:"amount"`
	Active           bool   `json:"active"`
}

type AccountDelegationsRequest struct {
	Address string `form:"address"`
	All     bool   `form:"all,default=false"`
	Page    int64  `form:"page,default=1"`
	Size    int64  `form:"size,default=5"`
}

type AccountDelegationsResponse struct {
	List []*AccountDelegationsInfo `json:"list"`
	Page
}

type AccountInfoRequest struct {
	Address string `path:"address"`
}

type AccountInfoResponse struct {
	Address    string              `json:"address"`
	Available  string              `json:"available"`
	Escrow     string              `json:"escrow"`
	Debonding  string              `json:"debonding"`
	Total      string              `json:"total"`
	Nonce      uint64              `json:"nonce"`
	Allowances []*AccountAllowance `json:"allowances"`
}

type AccountRewardExportRequest struct {
	Account string `form:"account"`
}

type AccountRewardExportResponse struct {
}

type AccountRewardInfo struct {
	ValidatorAddress string `json:"validatorAddress"`
	ValidatorName    string `json:"validatorName"`
	ValidatorIcon    string `json:"validatorIcon"`
	Epoch            int64  `json:"epoch"`
	Timestamp        int64  `json:"timestamp"`
	Reward           string `json:"reward"`
}

type AccountRewardRequest struct {
	Account string `form:"account"`
	Page    int64  `form:"page,default=1"`
	Size    int64  `form:"size,default=5"`
}

type AccountRewardResponse struct {
	List []AccountRewardInfo `json:"list"`
	Page
}

type AccountRewardStatsInfo struct {
	ValidatorName string                    `json:"validatorName"`
	RewardList    []*AccountRewardStatsItem `json:"rewardList"`
	Total         string                    `json:"total"`
}

type AccountRewardStatsItem struct {
	DateTime uint64 `json:"dateTime"`
	Reward   string `json:"reward"`
}

type AccountRewardStatsRequest struct {
	Account string `form:"account"`
}

type AccountRewardStatsResponse struct {
	Stats map[string]*AccountRewardStatsInfo `json:"stats"`
	Time  []int64                            `json:"time"`
}

type AccountStakingEventsInfo struct {
	Id     string `json:"id"`
	Height int64  `json:"height"`
	TxHash string `json:"txHash"`
	Kind   string `json:"kind"`
}

type AccountStakingEventsInfoRequest struct {
	Id string `form:"id"`
}

type AccountStakingEventsInfoResponse struct {
	Height          int64       `json:"height"`
	TxHash          string      `json:"txHash"`
	Kind            string      `json:"kind"`
	Timestamp       int64       `json:"timestamp"`
	Transafer       interface{} `json:"transafer,omitempty"`
	Burn            interface{} `json:"burn,omitempty"`
	Escrow          interface{} `json:"escrow,omitempty"`
	AllowanceChange interface{} `json:"allowanceChange,omitempty"`
}

type AccountStakingEventsRequest struct {
	Address string `form:"address"`
	Page    int64  `form:"page,default=1"`
	Size    int64  `form:"size,default=5"`
}

type AccountStakingEventsResponse struct {
	List []*AccountStakingEventsInfo `json:"list"`
	Page
}

type Bound struct {
	Start int64   `json:"start"`
	Min   float64 `json:"min"`
	Max   float64 `json:"max"`
}

type ChainBlockInfo struct {
	Height        int64  `json:"height"`
	Epoch         int64  `json:"epoch"`
	Timestamp     uint64 `json:"timestamp"`
	Time          uint64 `json:"time"`
	Hash          string `json:"hash"`
	Txs           int64  `json:"txs"`
	EntityAddress string `json:"entityAddress"`
	Name          string `json:"name"`
}

type ChainBlockInfoRequest struct {
	Height int64 `path:"height"`
}

type ChainBlockInfoResponse struct {
	ChainBlockInfo
}

type ChainBlocksRequest struct {
	Page int64 `form:"page,default=1"`
	Size int64 `form:"size,default=10"`
}

type ChainBlocksResponse struct {
	List []*ChainBlockInfo `json:"list"`
	Page
}

type ChainProposedBlocksRequest struct {
	Validator string `form:"validator,optional"`
	Page      int64  `form:"page,default=1"`
	Size      int64  `form:"size,default=5"`
}

type ChainProposedBlocksResponse struct {
	List []*ChainBlockInfo `json:"list"`
	Page
}

type ChainSearchRequest struct {
	Key string `form:"key"`
}

type ChainSearchResponse struct {
	Key    string `json:"key"`
	Type   string `json:"type"`
	Result string `json:"result"`
}

type ChainTransactionInfoRequest struct {
	Hash string `path:"hash"`
}

type ChainTransactionInfoResponse struct {
	TxHash       string `json:"txHash"`
	Timestamp    uint64 `json:"timestamp"`
	Time         uint64 `json:"time"`
	Height       int64  `json:"height"`
	Fee          string `json:"fee"`
	Nonce        int64  `json:"nonce"`
	Method       string `json:"method"`
	From         string `json:"from"`
	To           string `json:"to"`
	Amount       string `json:"amount"`
	Raw          string `json:"raw"`
	Status       bool   `json:"status"`
	ErrorMessage string `json:"errorMessage"`
}

type ChainTransactionListInfo struct {
	TxHash    string `json:"txHash"`
	Height    int64  `json:"height"`
	Method    string `json:"method"`
	Fee       string `json:"fee"`
	Amount    string `json:"amount"`
	Shares    string `json:"shares"`
	Add       bool   `json:"add"`
	Timestamp uint64 `json:"timestamp"`
	Time      uint64 `json:"time"`
	Status    bool   `json:"status"`
	From      string `json:"from"`
	To        string `json:"to"`
}

type ChainTransactionsRequest struct {
	Height  int64  `form:"height,optional"`
	Address string `form:"address,optional"`
	Method  string `form:"method,optional"`
	Page    int64  `form:"page,default=1"`
	Size    int64  `form:"size,default=10"`
}

type ChainTransactionsResponse struct {
	List []*ChainTransactionListInfo `json:"list"`
	Page
}

type Chart struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type DelegatorsInfo struct {
	Address string  `json:"address"`
	Amount  string  `json:"amount"`
	Shares  string  `json:"shares"`
	Percent float64 `json:"percent"`
	Self    bool    `json:"self"`
}

type DelegatorsRequest struct {
	Validator string `form:"validator"`
	Page      int64  `form:"page,default=1"`
	Size      int64  `form:"size,default=5"`
}

type DelegatorsResponse struct {
	List []*DelegatorsInfo `json:"list"`
	Page
}

type EscrowStatus struct {
	Self  string `json:"self"`
	Other string `json:"other"`
	Total string `json:"total"`
}

type GovernanceProposalInfo struct {
	Id        int64  `json:"id"`
	Title     string `json:"title"`
	Type      string `json:"type"`
	Submitter string `json:"submitter"`
	State     string `json:"state"`
	Deposit   string `json:"deposit"`
	CreatedAt int64  `json:"created_at"`
	ClosesAt  int64  `json:"closes_at"`
}

type GovernanceProposalListRequest struct {
}

type GovernanceProposalListResponse struct {
	List []*GovernanceProposalInfo `json:"list"`
}

type GovernanceProposalWithVotesRequest struct {
	Id int64 `form:"id"`
}

type GovernanceProposalWithVotesResponse struct {
	*GovernanceProposalInfo
	Options []*ProposalOption `json:"options"`
	Votes   []*ProposalVote   `json:"votes"`
}

type GovernanceVotesRequest struct {
	ProposalId       int64  `form:"proposalId,optional"`
	ValidatorAddress string `form:"validator,optional"`
	Page             int64  `form:"page,default=1"`
	Size             int64  `form:"size,default=5"`
}

type GovernanceVotesResponse struct {
	List []*ProposalVote `json:"list"`
	Page
}

type HealthRequest struct {
}

type HealthResponse struct {
}

type MarketChartRequest struct {
}

type MarketChartResponse struct {
	Price     []*Chart `json:"price"`
	MarketCap []*Chart `json:"marketCap"`
	Volume    []*Chart `json:"volume"`
}

type MarketInfoRequest struct {
}

type MarketInfoResponse struct {
	Price                 float64 `json:"price"`
	PriceChangePct24h     float64 `json:"priceChangePct24h"`
	Rank                  int64   `json:"rank"`
	MarketCap             int64   `json:"marketCap"`
	MarketCapChangePct24h float64 `json:"marketCapChangePct24h"`
	Volume                int64   `json:"volume"`
	VolumeChangePct24h    float64 `json:"volumeChangePct24h"`
}

type NetworkTrendRequest struct {
}

type NetworkTrendResponse struct {
	Tx     []*Chart `json:"tx"`
	Escrow []*Chart `json:"escrow"`
}

type Page struct {
	Page      int64 `json:"page"`
	Size      int64 `json:"size"`
	MaxPage   int64 `json:"maxPage"`
	TotalSize int64 `json:"totalSize"`
}

type ProposalOption struct {
	Name    string  `json:"name"`
	Amount  string  `json:"amount"`
	Percent float64 `json:"percent"`
}

type ProposalVote struct {
	ProposalId int64   `json:"proposalId"`
	Title      string  `json:"title"`
	Name       string  `json:"name"`
	Icon       string  `json:"icon"`
	Address    string  `json:"address"`
	Vote       string  `json:"vote"`
	Amount     string  `json:"amount"`
	Percent    float64 `json:"percent"`
}

type Rate struct {
	Start int64   `json:"start"`
	Rate  float64 `json:"rate"`
}

type RuntimeListInfo struct {
	RuntimeId string `json:"runtimeId"`
	Name      string `json:"name"`
}

type RuntimeListRequest struct {
}

type RuntimeListResponse struct {
	List []*RuntimeListInfo `json:"list"`
}

type RuntimeRoundInfo struct {
	Version      int64  `json:"version"`
	RuntimeId    string `json:"runtimeId"`
	RuntimeName  string `json:"runtimeName"`
	Round        int64  `json:"round"`
	Timestamp    int64  `json:"timestamp"`
	HeaderType   int64  `json:"header_type"`
	PreviousHash string `json:"previous_hash"`
	IoRoot       string `json:"io_root"`
	StateRoot    string `json:"state_root"`
	MessagesHash string `json:"messages_hash"`
	Next         bool   `json:"next"`
}

type RuntimeRoundInfoRequest struct {
	Id    string `form:"id"`
	Round int64  `form:"round"`
}

type RuntimeRoundInfoResponse struct {
	RuntimeRoundInfo
}

type RuntimeRoundListRequest struct {
	Id   string `form:"id"`
	Page int64  `form:"page,default=1"`
	Size int64  `form:"size,default=10"`
}

type RuntimeRoundListResponse struct {
	List []*RuntimeRoundInfo `json:"list"`
	Page
}

type RuntimeStatsInfo struct {
	EntityId  string            `json:"entityId"`
	Name      string            `json:"name"`
	Address   string            `json:"address"`
	Validator bool              `json:"validator"`
	Icon      string            `json:"icon"`
	Status    bool              `json:"status"`
	Stats     *RuntimeStatsItem `json:"stats"`
}

type RuntimeStatsItem struct {
	Elected  int64 `json:"elected"`
	Primary  int64 `json:"primary"`
	Backup   int64 `json:"backup"`
	Proposer int64 `json:"proposer"`
}

type RuntimeStatsRequest struct {
	Id string `form:"id"`
}

type RuntimeStatsResponse struct {
	Online  int64               `json:"online"`
	Offline int64               `json:"offline"`
	List    []*RuntimeStatsInfo `json:"list"`
}

type RuntimeTransactionConsensusTx struct {
	Method string `json:"method"`
	From   string `json:"from"`
	To     string `json:"to"`
	Amount string `json:"amount"`
	Nonce  int64  `json:"nonce"`
}

type RuntimeTransactionEventError struct {
	Code   int64  `json:"code"`
	Module string `json:"module"`
}

type RuntimeTransactionEvmTx struct {
	Hash     string `json:"hash"`
	From     string `json:"from"`
	To       string `json:"to"`
	Nonce    int64  `json:"nonce"`
	GasPrice int64  `json:"gasPrice"`
	GasLimit int64  `json:"gasLimit"`
	Data     string `json:"data"`
	Value    string `json:"value"`
}

type RuntimeTransactionInfoRequest struct {
	Id    string `form:"id"`
	Round int64  `form:"round"`
	Hash  string `form:"hash"`
}

type RuntimeTransactionInfoResponse struct {
	RuntimeId   string                         `json:"runtimeId"`
	RuntimeName string                         `json:"runtimeName"`
	TxHash      string                         `json:"txHash"`
	Round       int64                          `json:"round"`
	Result      bool                           `json:"result"`
	Message     string                         `json:"message"`
	Timestamp   uint64                         `json:"timestamp"`
	Type        string                         `json:"type"`
	Ctx         *RuntimeTransactionConsensusTx `json:"ctx"`
	Etx         *RuntimeTransactionEvmTx       `json:"etx"`
	Events      interface{}                    `json:"events"`
}

type RuntimeTransactionListInfo struct {
	RuntimeId string `json:"runtimeId"`
	TxHash    string `json:"txHash"`
	Round     int64  `json:"round"`
	Result    bool   `json:"result"`
	Timestamp uint64 `json:"timestamp"`
	Type      string `json:"type"`
}

type RuntimeTransactionListRequest struct {
	Id    string `form:"id"`
	Round int64  `form:"round,optional"`
	Page  int64  `form:"page,default=1"`
	Size  int64  `form:"size,default=10"`
}

type RuntimeTransactionListResponse struct {
	List []RuntimeTransactionListInfo `json:"list"`
	Page
}

type ValidatorBlocksStatsInfo struct {
	Height int64 `json:"height"`
	Block  bool  `json:"block"`
}

type ValidatorBlocksStatsRequest struct {
	Address string `form:"address"`
}

type ValidatorBlocksStatsResponse struct {
	Signs     []*ValidatorBlocksStatsInfo `json:"signs"`
	Proposals []*ValidatorBlocksStatsInfo `json:"proposals"`
}

type ValidatorEscrowStatsInfo struct {
	Timestamp int64  `json:"timestamp"`
	Escrow    string `json:"escrow"`
}

type ValidatorEscrowStatsRequest struct {
	Address string `form:"address"`
}

type ValidatorEscrowStatsResponse struct {
	List []*ValidatorEscrowStatsInfo `json:"list"`
}

type ValidatorInfo struct {
	Rank               int                 `json:"rank"`
	EntityId           string              `json:"entityId"`
	EntityAddress      string              `json:"entityAddress"`
	NodeId             string              `json:"nodeId"`
	NodeAddress        string              `json:"nodeAddress"`
	Name               string              `json:"name"`
	Icon               string              `json:"icon"`
	Website            string              `json:"website"`
	Twitter            string              `json:"twitter"`
	Keybase            string              `json:"keybase"`
	Email              string              `json:"email"`
	Description        string              `json:"description"`
	Escrow             string              `json:"escrow"`
	EscrowChange24     string              `json:"escrowChange24"`
	EscrowPercent      float64             `json:"escrowPercent"`
	Balance            string              `json:"balance"`
	TotalShares        string              `json:"totalShares"`
	Signs              int64               `json:"signs"`
	Proposals          int64               `json:"proposals"`
	Nonce              int64               `json:"nonce"`
	Score              int64               `json:"score"`
	Delegators         int64               `json:"delegators"`
	Nodes              []string            `json:"nodes"`
	Uptime             string              `json:"uptime"`
	Active             bool                `json:"active"`
	Commission         float64             `json:"commission"`
	Bound              *Bound              `json:"bound"`
	Rates              []Rate              `json:"rates"`
	Bounds             []Bound             `json:"bounds"`
	EscrowSharesStatus *EscrowStatus       `json:"escrowSharesStatus,omitempty"`
	EscrowAmountStatus *EscrowStatus       `json:"escrowAmountStatus,omitempty"`
	Runtimes           []*ValidatorRuntime `json:"runtimes"`
	Status             bool                `json:"status"`
}

type ValidatorInfoRequest struct {
	Address string `form:"address"`
}

type ValidatorInfoResponse struct {
	ValidatorInfo
}

type ValidatorListRequest struct {
	OrderBy string `form:"orderBy,default=escrow"`
	Sort    string `form:"sort,default=desc"`
}

type ValidatorListResponse struct {
	List       []ValidatorInfo `json:"list"`
	Active     int64           `json:"active"`
	Inactive   int64           `json:"inactive"`
	Delegators int64           `json:"delegators"`
}

type ValidatorRuntime struct {
	Name   string `json:"name"`
	Id     string `json:"id"`
	Online bool   `json:"online"`
}

type ValidatorSignStatsInfo struct {
	DateTime uint64 `json:"dateTime"`
	Expected uint64 `json:"expected"`
	Actual   uint64 `json:"actual"`
}

type ValidatorSignStatsRequest struct {
	Address string `form:"address"`
}

type ValidatorSignStatsResponse struct {
	Stats []*ValidatorSignStatsInfo `json:"stats"`
	Time  []int64                   `json:"time"`
}
