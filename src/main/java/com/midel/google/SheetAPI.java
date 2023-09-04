package com.midel.google;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SheetAPI {

    public static final Logger logger = LoggerFactory.getLogger(SheetAPI.class);
    public static Sheets sheetService;

    private static void getSheetService() {
        try {
            while (sheetService == null) {
                sheetService = GoogleAPIService.getSheetService();
                if (sheetService == null) {
                    TimeUnit.SECONDS.sleep(10);
                    logger.error("Failed to get sheet service.");
                }
            }
        } catch (InterruptedException | IOException | GeneralSecurityException e) {
            logger.error("Error while getting sheet service.", e);
        }
    }

    public static List<List<Object>> readSheetForRange(String spreadsheetId, String range) {

        try {
            getSheetService();

            ValueRange response = sheetService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();


            return response.getValues();

        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            logger.error("Failed read data for range: {}", error);
        } catch (IOException ee) {
            logger.error("Error while reading sheet for range. SheetID = {}, Range = {}", spreadsheetId, range, ee);
        }

        return null;
    }

    // Helper method to get the sheet ID from its name
    private static int getSheetId(String spreadsheetId, String sheetName) throws IOException {
        getSheetService();

        return sheetService.spreadsheets().get(spreadsheetId).execute()
                .getSheets().stream()
                .filter(sheet -> sheet.getProperties().getTitle().equals(sheetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sheet not found: " + sheetName))
                .getProperties().getSheetId();
    }

    // A helper method to get the row number for a given cell (for example, "A1")
    private static int getRowIndex(String cell) {
        return Integer.parseInt(cell.replaceAll("[^0-9]", "")) - 1;
    }

    // A helper method to get the column number for a given cell (for example, "A1")
    private static int getColumnIndex(String cell) {
        int column = 0;
        for (int i = 0; i < cell.length(); i++) {
            column *= 26;
            column += cell.charAt(i) - 'A' + 1;
        }
        return column - 1;
    }

    public static void mergeCells(String spreadsheetId, String sheetName, List<Pair<int[], int[]>> merges) {
        try {
            getSheetService();

            List<Request> requests = new ArrayList<>();
            Integer sheetId = getSheetId(spreadsheetId, sheetName);

            for (Pair<int[], int[]> merge : merges) {
                // Create a request object to merge cells
                MergeCellsRequest mergeRequest = new MergeCellsRequest();
                mergeRequest.setRange(new com.google.api.services.sheets.v4.model.GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(merge.getValue0()[0])
                                .setStartColumnIndex(merge.getValue0()[1])
                                .setEndRowIndex(merge.getValue1()[0] + 1)
                                .setEndColumnIndex(merge.getValue1()[1] + 1)
                        // Indexes not of the cells themselves, but of their borders
                );
                mergeRequest.setMergeType("MERGE_ALL"); // Specify the union type

                // Create a sheet change request object
                Request request = new Request();
                request.setMergeCells(mergeRequest);

                requests.add(request);
            }

            // Create a table change request object
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(requests);

            // Execute a query to change the table
            sheetService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();

        } catch (IOException e) {
            logger.error("Error while merging cells. SheetID = {}, Range = {}", spreadsheetId, e);
        }
    }

    public static void unmergeCells(String spreadsheetId, String sheetName, Pair<int[], int[]> range) {
        try {
            getSheetService();

            Integer sheetId = getSheetId(spreadsheetId, sheetName);

            // Create a request object to merge cells
            UnmergeCellsRequest unmergeRequest = new UnmergeCellsRequest();
            unmergeRequest.setRange(new com.google.api.services.sheets.v4.model.GridRange()
                            .setSheetId(sheetId)
                            .setStartRowIndex(range.getValue0()[0])
                            .setStartColumnIndex(range.getValue0()[1])
                            .setEndRowIndex(range.getValue1()[0] + 1)
                            .setEndColumnIndex(range.getValue1()[1] + 1)
                    // Indexes not of the cells themselves, but of their borders
            );


            Request request = new Request();
            request.setUnmergeCells(unmergeRequest);

            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            sheetService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();

        } catch (IOException e) {
            logger.error("Error while unmerging cells. SheetID = {}, Range = {}", spreadsheetId, e);
        }
    }

    public static boolean updateValues(String spreadsheetId, String range, List<List<Object>> values) {
        try {
            getSheetService();

            ValueRange vr = new ValueRange()
                    .setValues(values)
                    .setMajorDimension("ROWS");

            sheetService
                    .spreadsheets()
                    .values()
                    .update(spreadsheetId, range, vr)
                    .setValueInputOption("USER_ENTERED").execute();
            return true;
        } catch (IOException e) {
            logger.error("Error while updating sheet for range. SheetID = {}, Range = {}", spreadsheetId, range, e);
            return false;
        }
    }

    public static String createSpreadsheetFromTemplateAndSharePermission(String newSheetTitle, String[] shareList, String templateSheetId) {
        try {
            getSheetService();

            // Export template spreadsheet
            Spreadsheet sourceSpreadsheet = sheetService
                    .spreadsheets()
                    .get(templateSheetId)
                    .setIncludeGridData(true)
                    .execute();

            // Create a new spreadsheet based on template
            Spreadsheet spreadsheetClone = sourceSpreadsheet.clone();
            spreadsheetClone.setSpreadsheetId("").setSpreadsheetUrl("").getProperties().setTitle(newSheetTitle);

            Spreadsheet destinationSpreadsheet = sheetService
                    .spreadsheets()
                    .create(spreadsheetClone)
                    .setPrettyPrint(true)
                    .execute();

            logger.info("Spreadsheet successfully created and filled from template: {}", destinationSpreadsheet.getSpreadsheetUrl());

            // Give access to the new spreadsheet to users from shareList
            for (String user : shareList) {
                DriveAPI.shareWritePermission(destinationSpreadsheet.getSpreadsheetId(), user);
            }

            return destinationSpreadsheet.getSpreadsheetId();

        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 404) {
                logger.error("Spreadsheet not found with id {}", templateSheetId, e);
            } else {
                logger.error("Unknown error while creating and filling new sheet: ", e);
            }
        } catch (Exception e) {
            logger.error("Error while parsing from/to sheet or failed to update sheet title or protected range.", e);
        }

        return null;
    }

    public static void addNewTemplate(List<List<Object>> existData, String group, String sheetId) {
        boolean isExist = false;

        for (int i = 0; i < existData.size(); i++) {
            if (existData.get(i).size() != 2) {
                existData.remove(i);
                i--;
            } else {
                if (existData.get(i).get(0).equals(group)) {
                    existData.get(i).set(1, sheetId);
                    isExist = true;
                }
            }
        }

        if (!isExist) {
            existData.add(Arrays.asList(group, sheetId));
        }
    }
}