package svc

import (
	"fmt"
	_ "github.com/lib/pq"
	beacon "github.com/oasisprotocol/oasis-core/go/beacon/api"
	oasisGrpc "github.com/oasisprotocol/oasis-core/go/common/grpc"
	consensus "github.com/oasisprotocol/oasis-core/go/consensus/api"
	governance "github.com/oasisprotocol/oasis-core/go/governance/api"
	registry "github.com/oasisprotocol/oasis-core/go/registry/api"
	roothash "github.com/oasisprotocol/oasis-core/go/roothash/api"
	runtime "github.com/oasisprotocol/oasis-core/go/runtime/client/api"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/stores/sqlx"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"oasisscan-backend/job/internal/config"
	"oasisscan-backend/job/model"
)

type ServiceContext struct {
	Config                  config.Config
	PostgreDB               sqlx.SqlConn
	GrpcConn                *grpc.ClientConn
	Staking                 staking.Backend
	Consensus               consensus.ClientBackend
	Registry                registry.Backend
	Beacon                  beacon.Backend
	RootHash                roothash.Backend
	Runtime                 runtime.RuntimeClient
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
	StakingEventModel       model.StakingEventModel
	EscrowStatsModel        model.EscrowStatsModel
	RuntimeNodeModel        model.RuntimeNodeModel
	RuntimeStatsModel       model.RuntimeStatsModel
	ProposalModel           model.ProposalModel
}

func NewServiceContext(c config.Config) *ServiceContext {
	grpcConn, err := oasisGrpc.Dial(c.Node.Url, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		fmt.Errorf("grpc error: %v\n", err)
	}
	pgConn := sqlx.NewSqlConn("postgres", c.DataSource)
	return &ServiceContext{
		Config:                  c,
		PostgreDB:               pgConn,
		GrpcConn:                grpcConn,
		Staking:                 staking.NewStakingClient(grpcConn),
		Consensus:               consensus.NewConsensusClient(grpcConn),
		Registry:                registry.NewRegistryClient(grpcConn),
		Beacon:                  beacon.NewBeaconClient(grpcConn),
		RootHash:                roothash.NewRootHashClient(grpcConn),
		Runtime:                 runtime.NewRuntimeClient(grpcConn),
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
		StakingEventModel:       model.NewStakingEventModel(pgConn),
		EscrowStatsModel:        model.NewEscrowStatsModel(pgConn),
		RuntimeNodeModel:        model.NewRuntimeNodeModel(pgConn),
		RuntimeStatsModel:       model.NewRuntimeStatsModel(pgConn),
		ProposalModel:           model.NewProposalModel(pgConn),
	}
}
