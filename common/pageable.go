package common

import (
	"strconv"
	"strings"
)

type Pageable struct {
	LastID  int64
	Limit   int64
	Count   int64
	Offset  int64
	OrderBy string
}

func (p *Pageable) Empty() bool {
	if p == nil {
		return true
	}
	return p.LastID == 0 && p.Limit == 0 && p.Count == 0 && p.Offset == 0 && p.OrderBy == ""
}

func (p *Pageable) QueryLimit() string {
	if p.Limit == 0 && p.Offset == 0 {
		return ""
	}

	if p.Offset != 0 && p.Limit == 0 {
		return ""
	}

	var builder strings.Builder
	builder.Grow(10)
	builder.WriteString(" limit ")
	if p.Offset != 0 {
		builder.WriteString(strconv.FormatInt(p.Offset, 10))
		builder.WriteString(",")
	}
	if p.Limit != 0 {
		builder.WriteString(strconv.FormatInt(p.Limit, 10))
	}
	return builder.String()
}

func MaxPage(pageSize, totalSize int64) int64 {
	if totalSize%pageSize == 0 {
		return totalSize / pageSize
	} else {
		return totalSize/pageSize + 1
	}
}

func PageLimit[T any](dataList []T, pageNum, pageSize int64) []T {
	if pageNum < 1 {
		return []T{}
	}
	start := (pageNum - 1) * pageSize
	if start >= int64(len(dataList)) {
		return []T{}
	}
	end := start + pageSize
	if end > int64(len(dataList)) {
		end = int64(len(dataList))
	}
	return dataList[start:end]
}
