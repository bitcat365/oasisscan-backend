.PHONY: fmt vet gen

fmt:
	@go fmt ./...

vet:
	@go vet ./...

gen:
	@goctl rpc protoc -I . -I .. -I ../.. ./proto/job.proto --go_out=./ --go-grpc_out=./ --zrpc_out=./ --style=go_zero --go_opt=paths=source_relative --go-grpc_opt=paths=source_relative
	@goctl docker -go job.go

build:
	@GOOS=linux GOARCH=amd64 go build -ldflags="-w -s" -o bin/server job.go
