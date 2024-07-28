package svc

import (
	_ "github.com/lib/pq"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	oasisGrpc "github.com/oasisprotocol/oasis-core/go/common/grpc"
	consensus "github.com/oasisprotocol/oasis-core/go/consensus/api"
	governance "github.com/oasisprotocol/oasis-core/go/governance/api"
	registry "github.com/oasisprotocol/oasis-core/go/registry/api"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logx"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"oasisscan-backend/api/internal/config"
	"oasisscan-backend/common/coingecko"
	"oasisscan-backend/job/model"
)

type ServiceContext struct {
	Config                  config.Config
	PostgreDB               sqlx.SqlConn
	Staking                 staking.Backend
	Consensus               consensus.ClientBackend
	Registry                registry.Backend
	Beacon                  beacon.Backend
	Governance              governance.Backend
	BlockModel              model.BlockModel
	BlockSignatureModel     model.BlockSignatureModel
	TransactionModel        model.TransactionModel
	SystemPropertyModel     model.SystemPropertyModel
	NodeModel               model.NodeModel
	ValidatorModel          model.ValidatorModel
	DelegatorModel          model.DelegatorModel
	RewardModel             model.RewardModel
	RuntimeModel            model.RuntimeModel
	RuntimeRoundModel       model.RuntimeRoundModel
	RuntimeTransactionModel model.RuntimeTransactionModel
	RuntimeNodeModel        model.RuntimeNodeModel
	RuntimeStatsModel       model.RuntimeStatsModel
	StakingEventModel       model.StakingEventModel
	EscrowStatsModel        model.EscrowStatsModel
	ProposalModel           model.ProposalModel
	VoteModel               model.VoteModel
	CoingeckoClient         coingecko.CoingeckoClient
	LocalCache              *LocalCache
}

func NewServiceContext(c config.Config) *ServiceContext {
	grpcConn, err := oasisGrpc.Dial(c.Node.Url, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		logx.Errorf("grpc error: %v\n", err)
	}
	pgConn := sqlx.NewSqlConn("postgres", c.DataSource)
	return &ServiceContext{
		Config:                  c,
		PostgreDB:               pgConn,
		Staking:                 staking.NewStakingClient(grpcConn),
		Consensus:               consensus.NewConsensusClient(grpcConn),
		Registry:                registry.NewRegistryClient(grpcConn),
		Beacon:                  beacon.NewBeaconClient(grpcConn),
		Governance:              governance.NewGovernanceClient(grpcConn),
		BlockModel:              model.NewBlockModel(pgConn),
		BlockSignatureModel:     model.NewBlockSignatureModel(pgConn),
		TransactionModel:        model.NewTransactionModel(pgConn),
		SystemPropertyModel:     model.NewSystemPropertyModel(pgConn),
		NodeModel:               model.NewNodeModel(pgConn),
		ValidatorModel:          model.NewValidatorModel(pgConn),
		DelegatorModel:          model.NewDelegatorModel(pgConn),
		RewardModel:             model.NewRewardModel(pgConn),
		RuntimeModel:            model.NewRuntimeModel(pgConn),
		RuntimeRoundModel:       model.NewRuntimeRoundModel(pgConn),
		RuntimeTransactionModel: model.NewRuntimeTransactionModel(pgConn),
		RuntimeNodeModel:        model.NewRuntimeNodeModel(pgConn),
		RuntimeStatsModel:       model.NewRuntimeStatsModel(pgConn),
		StakingEventModel:       model.NewStakingEventModel(pgConn),
		EscrowStatsModel:        model.NewEscrowStatsModel(pgConn),
		ProposalModel:           model.NewProposalModel(pgConn),
		VoteModel:               model.NewVoteModel(pgConn),
		CoingeckoClient:         coingecko.NewCoingeckoClient(),
		LocalCache:              NewLocalCache(),
	}
}
