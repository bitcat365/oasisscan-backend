package common

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/oasisprotocol/oasis-core/go/common/crypto/signature"
	staking "github.com/oasisprotocol/oasis-core/go/staking/api"
	"github.com/zeromicro/go-zero/core/logc"
	"io"
	"net/http"
	"net/url"
	"strconv"
)

func HttpRequest(apiUrl string, method string, params map[string]interface{}) ([]byte, error) {
	var err error
	if err != nil {
		return nil, err
	}
	var req *http.Request
	if method == "GET" {
		requestUrl := apiUrl
		if params != nil {
			values := url.Values{}
			for k, v := range params {
				values.Add(k, v.(string))
			}
			requestUrl = apiUrl + "?" + values.Encode()
		}
		req, _ = http.NewRequest(method, requestUrl, nil)
	} else if method == "POST" {
		var jsonStr []byte
		jsonStr, err = json.Marshal(params)
		req, _ = http.NewRequest(method, apiUrl, bytes.NewBuffer(jsonStr))
		req.Header.Set("Content-Type", "application/json")
	} else {
		return nil, errors.New("unknown http method")
	}

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	content, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	return content, nil
}

func PubKeyToBech32Address(ctx context.Context, base64str string) (*staking.Address, error) {
	var pubKey signature.PublicKey
	err := pubKey.UnmarshalText([]byte(base64str))
	if err != nil {
		logc.Errorf(ctx, "base64 decode error: %s, %v", base64str, err)
		return nil, err
	}
	address := staking.NewAddress(pubKey)
	return &address, nil
}

func GetNestedStringValue(data interface{}, keys ...string) (string, error) {
	for _, key := range keys {
		switch value := data.(type) {
		case map[string]interface{}:
			if v, ok := value[key]; ok {
				data = v
			} else {
				return "", fmt.Errorf("key %s not found", key)
			}
		case []interface{}:
			if index, err := strconv.Atoi(key); err == nil && index >= 0 && index < len(value) {
				data = value[index]
			} else {
				return "", fmt.Errorf("key %s not found", key)
			}
		default:
			return "", fmt.Errorf("unexpected data type")
		}
	}
	if value, ok := data.(string); ok {
		return value, nil
	}
	return "", fmt.Errorf("key %s contains non-string value", keys[len(keys)-1])
}
