package common

import (
	"math"
	"math/big"
	"regexp"
	"strconv"
	"strings"
)

func ValueToFloatByDecimals(value *big.Int, decimals int64) *big.Float {
	return new(big.Float).Quo(new(big.Float).SetInt(value), big.NewFloat(math.Pow(10, float64(decimals))))
}

func IsInteger(s string) bool {
	_, err := strconv.Atoi(s)
	return err == nil
}

func IsHex(s string) bool {
	if strings.HasPrefix(s, "0x") {
		s = strings.ReplaceAll(s, "0x", "")
	}
	matched, _ := regexp.MatchString("^[0-9A-Fa-f]+$", s)
	return matched
}
