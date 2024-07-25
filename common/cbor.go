package common

import (
	"fmt"
	"github.com/fxamacker/cbor/v2"
)

var (
	decOptions = cbor.DecOptions{
		DupMapKey:         cbor.DupMapKeyEnforcedAPF,
		IndefLength:       cbor.IndefLengthForbidden,
		TagsMd:            cbor.TagsForbidden,
		ExtraReturnErrors: cbor.ExtraDecErrorNone,
		MaxArrayElements:  10_000_000, // Usually limited by blob size limits anyway.
		MaxMapPairs:       10_000_000, // Usually limited by blob size limits anyway.
	}
)

func CborUnmarshalOmit(data []byte, dst interface{}) error {
	if data == nil {
		return nil
	}

	decMode, err := decOptions.DecMode()
	if err != nil {
		fmt.Println("Error creating decoder mode:", err)
		return err
	}

	return decMode.Unmarshal(data, dst)
}
