package com.midel.pdf;

import com.aspose.cells.Cell;
import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import com.midel.generics.Common;
import com.midel.generics.SubGroupPair;
import com.midel.group.Day;
import com.midel.group.Subj;
import com.midel.group.Week;
import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TableController {

    protected static List<List<String>> parseTableFromPDF(PDDocument document, int pageIndex) {
        List<List<String>> result = new ArrayList<>();

        SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();

        Page page = new ObjectExtractor(document).extract(pageIndex);

        List<Table> table = sea.extract(page);

        // iterate over the tables of the page
        for (Table tables : table) {
            List<List<RectangularTextContainer>> rows = tables.getRows();
            // iterate over the rows of the table
            for (List<RectangularTextContainer> cells : rows) {
                List<String> line = new ArrayList<>();

                int emptyElementsInRow = 0;

                for (RectangularTextContainer content : cells) {
                    // Note: Cell.getText() uses \r to concat text chunks
                    List<?> textElements = content.getTextElements();
                    List<String> temp = textElements.stream().map(e -> replaceSimilarLetter(((TextChunk) e).getText())).collect(Collectors.toList());
                    if (!temp.isEmpty()) {
                        line.addAll(temp);
                        emptyElementsInRow = 0;
                    } else {
                        if (!line.isEmpty()) {
                            line.add("");
                            emptyElementsInRow++;
                        }
                    }
                }

                line = line.subList(0, line.size() - emptyElementsInRow);

                for (int i = 0; i < line.size(); i++) {
                    String cell = line.get(i);
                    for (String role : Subj.roles) {
                        if (cell.contains(role)) {
                            line.set(i, role);
                            String temp = cell.replace(role, "").trim();
                            if (temp.length() > 0) {
                                line.add(i + 1, temp);
                            }
                            break;
                        }
                    }
                }
                result.add(line);

            }
        }
        return result;
    }

    public static OutputStream convertExcelToPDF(InputStream inputStream) throws Exception {
        com.aspose.cells.Workbook workbook = new Workbook(inputStream);

        Cell cell = null;
        int pageCount = -1;
        do {
            cell = workbook.getWorksheets().get(0).getCells().find("НАУ", cell);
            pageCount++;
        } while (cell != null);

        PdfSaveOptions options = new PdfSaveOptions();
        options.setDefaultFont("arial");
        //options.setAllColumnsInOnePagePerSheet(true);
        //options.setExportDocumentStructure(true);
        //options.setPrintingPageType();
        options.setPageCount(pageCount);

        OutputStream os = new ByteArrayOutputStream();
        workbook.save(os, options);
        return os;
        //workbook.save(outputPath, options);
    }

    protected static Week getWeekFromTable(List<List<String>> tableData) {

        Week week = new Week(new LinkedHashMap<>());
        for (int dw = 0; dw < 6; dw++) {

            List<String> subjectLine;
            Day day = new Day(new HashMap<>());
            String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};

            for (int subj = dw * 6 + 3; subj < dw * 6 + 6 + 3; subj++) {
                subjectLine = tableData.get(subj);

                if (subjectLine.get(0).length() > 2) {
                    subjectLine.remove(0);
                }
                System.out.println(subjectLine + "->" + subjectLine.size());

                Subj firstSubj = null;
                Subj secondSubj = null;
                String nameLesson;
                String type;
                String lector;
                String auditory;

                // If the pair has one in common, but has a size of 7-10, then most likely the teacher split it into two elements
                if (subjectLine.size() >= 7 && subjectLine.size() < 11 && !subjectLine.get(1).isEmpty()) {

                    boolean isEmptyElementReason = false;
                    for (int i = 2; i < subjectLine.size(); i++) {
                        if (subjectLine.get(i).isEmpty()) {
                            subjectLine.remove(i);
                            isEmptyElementReason = true;
                            i--;
                        }
                    }

                    if (!isEmptyElementReason) {
                        subjectLine.set(
                                subjectLine.size() - 2,
                                subjectLine.get(subjectLine.size() - 2) + " " + subjectLine.get(subjectLine.size() - 1)
                        );
                        subjectLine.remove(subjectLine.size() - 1);
                    }
                }

                switch (subjectLine.size()) {
                    // No lessons
                    case 1: {
                        firstSubj = null;
                        secondSubj = null;
                        break;
                    }
                    // the lesson is together, but without a teacher and an audience
                    case 4: {
                        nameLesson = subjectLine.get(1);
                        type = subjectLine.get(3);//.replace("i", "і");
                        lector = null;
                        auditory = null;

                        firstSubj = new Subj(nameLesson, type, lector, auditory);
                        secondSubj = firstSubj;
                        break;
                    }
                    // lesson only in the first subgroup / common lesson
                    case 6: {
                        type = subjectLine.get(3);//.replace("i", "і");
                        if (type.equals("Практичне") || type.equals("Лекція")) {
                            nameLesson = subjectLine.get(1);
                            auditory = subjectLine.get(2).replace(" ", "");
                            lector = subjectLine.get(5);

                            firstSubj = new Subj(nameLesson, type, lector, auditory);
                            secondSubj = firstSubj;
                        } else {
                            nameLesson = subjectLine.get(1);
                            auditory = subjectLine.get(2).replace(" ", "");
                            lector = subjectLine.get(5);
                            firstSubj = new Subj(nameLesson, type, lector, auditory);
                            secondSubj = null;
                        }
                        break;
                    }
                    // only in the second subgroup
                    case 7: {
                        nameLesson = subjectLine.get(2);
                        auditory = subjectLine.get(3).replace(" ", "");
                        type = subjectLine.get(4);//replace("i", "і");
                        lector = subjectLine.get(6);

                        firstSubj = null;
                        secondSubj = new Subj(nameLesson, type, lector, auditory);
                        break;
                    }
                    // Different pairs for subgroups
                    case 11: {
                        nameLesson = subjectLine.get(1);
                        auditory = subjectLine.get(2).replace(" ", "");
                        type = subjectLine.get(3);//replace("i", "і");
                        lector = subjectLine.get(5);

                        firstSubj = new Subj(nameLesson, type, lector, auditory);

                        nameLesson = subjectLine.get(6);
                        auditory = subjectLine.get(7).replace(" ", "");
                        type = subjectLine.get(8);//replace("i", "і");
                        lector = subjectLine.get(10);
                        secondSubj = new Subj(nameLesson, type, lector, auditory);
                        break;
                    }
                }


                if (firstSubj == null && secondSubj == null) {
                    day.lessons.put(((subj - 3) % 6 + 1) + "", new Common<>(null));
                } else if (firstSubj == secondSubj) {
                    day.lessons.put(((subj - 3) % 6 + 1) + "", new Common<>(firstSubj));
                } else {
                    day.lessons.put(((subj - 3) % 6 + 1) + "", new SubGroupPair<>(firstSubj, secondSubj));
                }
            }
            week.days.put(days[dw], day);
        }

        return week;
    }

    public static String replaceSimilarLetter(String str) {

        int cyrillicCount = 0;
        int englishCount = 0;

        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                    cyrillicCount++;
                } else if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN) {
                    englishCount++;
                }
            }
        }

        if (englishCount > cyrillicCount) {
            return str;
        }

        Map<Character, Character> charReplacementMap = new HashMap<>();
        charReplacementMap.put('A', 'А');
        charReplacementMap.put('a', 'а');
        charReplacementMap.put('B', 'В');
        charReplacementMap.put('C', 'С');
        charReplacementMap.put('c', 'с');
        charReplacementMap.put('E', 'Е');
        charReplacementMap.put('e', 'е');
        charReplacementMap.put('H', 'Н');
        charReplacementMap.put('I', 'І');
        charReplacementMap.put('i', 'і');
        charReplacementMap.put('K', 'К');
        charReplacementMap.put('M', 'М');
        charReplacementMap.put('O', 'О');
        charReplacementMap.put('o', 'о');
        charReplacementMap.put('P', 'Р');
        charReplacementMap.put('p', 'р');
        charReplacementMap.put('X', 'Х');
        charReplacementMap.put('x', 'х');
        charReplacementMap.put('y', 'у');


        // Walk through the string and replace characters based on their similarity to dictionary keys
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (charReplacementMap.containsKey(c)) {
                // If a similar letter is found in the dictionary, replace it
                result.append(charReplacementMap.get(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
