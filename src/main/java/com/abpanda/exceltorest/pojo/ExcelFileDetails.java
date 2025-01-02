package com.abpanda.exceltorest.pojo;

import lombok.Data;

@Data
public class ExcelFileDetails {
    private String filePath;
    public int rowsToProcess;
    private String hostname;
    private String port;
}
