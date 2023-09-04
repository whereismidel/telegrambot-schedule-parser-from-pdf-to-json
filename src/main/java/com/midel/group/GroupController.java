package com.midel.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.midel.pdf.TableController;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class GroupController extends TableController {
    public static HashMap<String, Group> parseGroupsFromPDF(InputStream inputStream) throws IOException {

        Group.groups = new HashMap<>();

        try (PDDocument document = PDDocument.load(inputStream)) {
            int pageCount = document.getNumberOfPages();

            for (int i = 1; i <= pageCount; i++) {

                TableController.parseTableFromPDF(document, i);

                List<List<String>> tableData = TableController.parseTableFromPDF(document, i);


                List<String> header = tableData.get(0);

                // get group name from header
                // [НАУ, ФКПІ 371, Cybersecurity systems and technologies, Тиждень 1]
                String groupName = header.get(1);

                // get spec from header
                String spec;
                spec = header.get(2);

                // get week
                String weekNum = header.get(3).replace("Тиждень", "").trim();

                Group group;
                if (!Group.groups.containsKey(groupName)) {
                    Group.groups.put(groupName, new Group(groupName, spec, new LinkedHashMap<>()));
                    group = Group.groups.get(groupName);

                    group.weeks.put("firstWeek", null);
                    group.weeks.put("secondWeek", null);
                }
                group = Group.groups.get(groupName);

                Week week = TableController.getWeekFromTable(tableData);
                group.weeks.put(weekNum.equals("1") ? "firstWeek" : "secondWeek", week);
            }
        }

        return Group.groups;
    }

    public static Pair<OutputStream, String> groupToJson(Group group, boolean toFile) throws IOException {
        if (group.weeks.get("firstWeek") != null && group.weeks.get("secondWeek") != null &&
                !group.group.isEmpty() && !group.spec.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            String json = writer.writeValueAsString(group);

            if (toFile) {
                //String resultPath = "src/main/resources/json/" + group.group.replace(" ", "_") + ".json";
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    outputStream.write(json.getBytes());

                    return new Pair<>(outputStream, json);
                }
            }

            return new Pair<>(null, json);
        }

        return null;
    }

    public static Group jsonToGroup(InputStream inputStream) throws IOException {
        return new ObjectMapper().readValue(inputStream, Group.class);
    }
}
