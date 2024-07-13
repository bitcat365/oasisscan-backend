package config

import "github.com/zeromicro/go-zero/rest"

type Config struct {
	rest.RestConf
	DataSource string
	Node       NodeConf
}

type NodeConf struct {
	Url string
}
