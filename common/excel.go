package common

import (
	"bytes"
	"fmt"
	"github.com/xuri/excelize/v2"
)

func ToExcel(titleList []string, data [][]string) (content []byte, err error) {
	file := excelize.NewFile()
	//sheet
	sheet, err := file.NewSheet("Sheet1")
	if err != nil {
		return nil, fmt.Errorf("sheet error: %v", err)
	}
	file.SetActiveSheet(sheet)
	//title
	for i, v := range titleList {
		cellName, _ := excelize.CoordinatesToCellName(i+1, 1)
		err = file.SetCellValue("Sheet1", cellName, v)
		if err != nil {
			return nil, fmt.Errorf("cell error: %v", err)
		}
	}
	//data
	for i, d := range data {
		row := i + 2
		cells := make([]interface{}, 0)
		for _, v := range d {
			cells = append(cells, v)
		}
		if err := file.SetSheetRow("Sheet1", fmt.Sprintf("A%d", row), &cells); err != nil {
			return nil, err
		}
	}

	var buffer bytes.Buffer
	_ = file.Write(&buffer)
	return buffer.Bytes(), nil
}
