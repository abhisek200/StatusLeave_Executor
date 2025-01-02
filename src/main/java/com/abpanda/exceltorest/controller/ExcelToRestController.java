package com.abpanda.exceltorest.controller;

import com.abpanda.exceltorest.pojo.ExcelFileDetails;
import com.abpanda.exceltorest.service.ExcelToRestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/v1/convert")
public class ExcelToRestController {

    private ExcelToRestService excelToRestService;

    public ExcelToRestController(ExcelToRestService excelToRestService) {
        this.excelToRestService = excelToRestService;
    }

    @PostMapping
    public ResponseEntity<String> convert(@RequestBody ExcelFileDetails excelFileDetails) {
        System.out.println("Received convert request");
        boolean success;

        try {

            success = excelToRestService.convert(excelFileDetails);

        }catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!success) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            return ResponseEntity.ok()
                    .body("Converted and fired successfully");
        }
    }

}
