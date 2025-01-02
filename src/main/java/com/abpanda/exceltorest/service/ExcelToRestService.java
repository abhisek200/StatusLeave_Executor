package com.abpanda.exceltorest.service;

//How to run?
//        java -jar ExcelToRest-0.0.1-SNAPSHOT.jar
//
//        This will start a standalone spring boot application.
//        Next you need to pass file details and host details by firing a POST API call: -
//        URL : - localhost:8080/v1/convert
//        Body: -
//        {
//        "filePath": "<path>/edg-prod-export1.xlsx",
//        "rowsToProcess": 1000,
//        "hostname": "localhost",
//        "port": "1512"
//        }

import com.abpanda.exceltorest.pojo.ExcelFileDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class ExcelToRestService {

    // Method to read data from the Excel file
    public static List<Map<String, String>> readExcelFile(String filePath) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(new File(filePath));
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(0);
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Map<String, String> rowData = new HashMap<>();
            for (int j = 0; j < row.getLastCellNum(); j++) {
                String header = headerRow.getCell(j).getStringCellValue();
                Cell cell = row.getCell(j);
                String value = cell != null ? cell.toString() : "";
                rowData.put(header, value);
            }
            rows.add(rowData);
        }

        workbook.close();
        fileInputStream.close();
        return rows;
    }

    // Method to construct the JSON payload
    public static String createJsonPayload(Map<String, String> rowData) {
        String processId = rowData.get("ProcessIdentifier");
        String workflowId = rowData.get("WorkflowIdentifier");
        String status = rowData.get("Status");
        String entityProxy = rowData.get("Identifier"); // Example: "104532@1001:1000"
        String[] divide = entityProxy.split(":");
        String itemId = divide[0]; // Example: "104532@1001"
        String entityId = divide[1]; // Example: "1000"

        // Resolve entity based on entityId
        String entity;
        switch (entityId) {
            case "1000":
                entity = "Article";
                break;
            case "1100":
                entity = "Product";
                break;
            case "1200":
                entity = "Variant";
                break;
            default:
                throw new IllegalArgumentException("Unknown entityId: " + entityId);
        }

        // Construct the JSON payload
        String payload = "{"
                + "\"processId\": \"" + processId + "\","
                + "\"workflowId\": \"" + workflowId + "\","
                + "\"status\": \"" + status + "\","
                + "\"entity\": \"" + entity + "\","
                + "\"hint\": \"via customCode\","
                + "\"itemId\": [\"" + itemId + "\"]"
                + "}";

        log.info("payload: {}", payload);
        return payload;
    }

    // Method to perform a POST request with Basic Authentication
    public static void makePostRequest(String url, String jsonPayload) throws IOException {
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        // Add Basic Authentication header
        ExcelFileDetails ex = new ExcelFileDetails();
        String username = ex.username;
        String password = ex.password;
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("POST Success: " + jsonPayload);
        } else {
            System.err.println("POST Failed with response code: " + responseCode);
        }
        connection.disconnect();
    }

    // Orchestrator method
    public static void processExcelAndPost(String filePath, String apiUrl, int rowsToProcess) throws IOException {

        List<Map<String, String>> rows = readExcelFile(filePath);

        // Prompt the user for the row limit
//            Scanner scanner = new Scanner(System.in);
//            System.out.println("Enter the number of rows to process (e.g., 1000):");
//            int rowsToProcess = scanner.nextInt();
//            scanner.close();

        // Ensure the limit is within bounds
        rowsToProcess = Math.min(rowsToProcess, rows.size());

        // Loop through the rows, up to the user-defined limit
        for (int i = 0; i < rowsToProcess; i++) {
            Map<String, String> row = rows.get(i);
            String jsonPayload = createJsonPayload(row);
            makePostRequest(apiUrl, jsonPayload);
        }

    }

    /**
     * Prints the introduction and required SQL query details.
     */
    public static void printIntroduction() {
        System.out.println("**************************************************************************");
        System.out.println("******************** StatusLeave_BulkExecutor **********************");
        System.out.println("**************************************************************************");
        System.out.println("Your Excel should be output of the following Query: ");
        System.out.println("SELECT \n" +
                "    sr.[Identifier] AS Identifier,\n" +
                "    pse.[ProcessIdentifier] AS ProcessIdentifier,\n" +
                "    pse.[Workflow2GID] AS Workflow2GID,\n" +
                "    pse.[Status] AS Status,\n" +
                "    wf.[Identifier] AS WorkflowIdentifier\n" +
                "FROM \n" +
                "    [PIM_MAIN].[dbo].[ProcessStatusEntry] pse\n" +
                "JOIN \n" +
                "    [PIM_MAIN].[dbo].[StatusRevision] sr ON pse.[StatusRevisionID] = sr.[ID]\n" +
                "JOIN \n" +
                "    [PIM_MAIN].[dbo].[Workflow2G] wf ON pse.[Workflow2GID] = wf.[ID]\n" +
                "WHERE \n" +
                "    pse.[UserID] IS NULL <Conditional/> \n" +
                "    AND pse.[UserGroupID] IS NULL <Conditional/> \n" +
                "    AND pse.[Status] = 'Check Staging Data Admin'; <optional/> ");

        System.out.println("**************************************************************************");
        System.out.println("******************** StatusLeave_BulkExecutor **********************");
        System.out.println("**************************************************************************");

    }

    /**
     * Constructs the API URL from the provided hostname and port.
     *
     * @param hostname the hostname provided by the user
     * @param port     the port number provided by the user
     * @return the constructed API URL
     */
    private static String constructApiUrl(String hostname, String port) {
        return "http://" + hostname + ":" + port + "/rest/V1.0/manage/workflow/status/leave";
    }

    // Main method
    public boolean convert(ExcelFileDetails ex) throws IOException {
        // Display introductory information
//        printIntroduction();

        // Create a Scanner for user input
//        Scanner scanner = new Scanner(System.in);

        // Collect inputs from the user
//        String excelFilePath = getUserInput(scanner, "Enter the exact file path (e.g., C:\\files\\EDG_PRD_Completelist.xlsx):");
//        String hostname = getUserInput(scanner, "Enter the hostname (e.g., localhost):");
//        String port = getUserInput(scanner, "Enter the port number (e.g., 1512):");


        // Construct the API URL
        String apiUrl = constructApiUrl(ex.getHostname(), ex.getPort());

        // Display the collected information
        log.info("\nCollected Information:");
        log.info("File Path: " + ex.getFilePath());
        log.info("API URL: " + apiUrl);

        // Process the Excel file and send POST requests
        processExcelAndPost(ex.getFilePath(), apiUrl, ex.getRowsToProcess());

        return true;

        // Close the Scanner
//        scanner.close();
    }
}
