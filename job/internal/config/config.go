package config

import (
	"github.com/zeromicro/go-zero/zrpc"
)

type Config struct {
	zrpc.RpcServerConf
	DataSource string
	Node       NodeConf
	Validator  ValdatorConf
}

type NodeConf struct {
	Url string
}

type ValdatorConf struct {
	GitInfo     string
	KeybaseJson string
}
