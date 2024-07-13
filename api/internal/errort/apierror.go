package errort

import (
	"context"
	"net/http"
)

func ErrorHandler() func(ctx context.Context, err error) (int, interface{}) {
	return func(ctx context.Context, err error) (int, interface{}) {
		if err == nil {
			return http.StatusOK, CodeErrorResponse{}
		}
		e, ok := err.(*CodeError)
		if ok {
			switch e.Code {
			case Unauthorized:
				return http.StatusUnauthorized, e.Data()
			}
			return http.StatusOK, e.Data()
		}
		return http.StatusBadRequest, nil
	}
}

type CodeError struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
}

type CodeErrorResponse struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
}

func NewCodeError(code int, msg string) error {
	return &CodeError{Code: code, Msg: msg}
}

func UnauthorizedError(msg string) error {
	return &CodeError{Code: Unauthorized, Msg: msg}
}

func NewDefaultError() error {
	return NewCodeError(InternalErrCode, "internal error")
}

func (e *CodeError) Error() string {
	return e.Msg
}

func (e *CodeError) Data() *CodeErrorResponse {
	return &CodeErrorResponse{
		Code: e.Code,
		Msg:  e.Msg,
	}
}
