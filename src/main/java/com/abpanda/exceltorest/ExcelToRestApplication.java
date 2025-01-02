package com.abpanda.exceltorest;

import com.abpanda.exceltorest.service.ExcelToRestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExcelToRestApplication {

    public static void main(String[] args) {
        ExcelToRestService ex = new ExcelToRestService();
        ex.printIntroduction();
        SpringApplication.run(ExcelToRestApplication.class, args);
    }

}
