.PHONY: fmt vet gen

fmt:
	@go fmt ./...

vet:
	@go vet ./...

build:
	@GOOS=linux GOARCH=amd64 go build -ldflags="-w -s" -o bin/server oasisscan.go