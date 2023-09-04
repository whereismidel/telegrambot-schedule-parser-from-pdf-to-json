package com.midel.google;

import com.midel.generics.Common;
import com.midel.generics.SubGroupPair;
import com.midel.group.Day;
import com.midel.group.Group;
import com.midel.group.Subj;
import com.midel.group.Week;
import org.javatuples.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.midel.BotConfig.*;

class SubjectInfo {
    public String name;
    public String type;

    public boolean splitOnSub;

    public String commonLector;
    public String firstGroupLector;
    public String secondGroupLector;

    public String commonAuditory;
    public String firstGroupAuditory;
    public String secondGroupAuditory;

    @Override
    public String toString() {
        return "SubjectInfo{\n" +
                "\tname=" + name + "\n" +
                "\ttype=" + type + "\n" +
                "\tsplitOnSub=" + splitOnSub + "\n" +
                "\tcommonLector=" + commonLector +
                ", commonAuditory=" + commonAuditory + "\n" +
                "\tfirstGroupLector=" + firstGroupLector +
                ", firstGroupAuditory=" + firstGroupAuditory + "\n" +
                "\tsecondGroupLector=" + secondGroupLector +
                ", secondGroupAuditory=" + secondGroupAuditory + "\n" +
                '}';
    }
}

public class CreateTable {
    public static String createSheetFromGroup(Group group, List<List<Object>> existGroupsLinkData) throws IOException {
        String sheetId = SheetAPI.createSpreadsheetFromTemplateAndSharePermission("Шаблон " + group.group, new String[]{GMAIL_ACCOUNT_ADMIN}, CLEAN_TEMPLATE);

        SheetAPI.addNewTemplate(existGroupsLinkData, group.group, sheetId);
        SheetAPI.updateValues(ADMIN_INFO_SHEET, "GroupTemplateLink!A1:B" + existGroupsLinkData.size(), existGroupsLinkData);

        List<List<Object>> infoData = SheetAPI.readSheetForRange(sheetId, "ПРЕДМЕТИ!A1:O31");
        CreateTable.fillInfo(group, infoData);
        SheetAPI.updateValues(sheetId, "ПРЕДМЕТИ!A1:O31", infoData);

        List<List<Object>> scheduleData = SheetAPI.readSheetForRange(sheetId, "РОЗКЛАД!A1:T36");
        List<Pair<int[], int[]>> merges = CreateTable.fillSchedule(group, scheduleData);
        SheetAPI.updateValues(sheetId, "РОЗКЛАД!A1:T36", scheduleData);
        SheetAPI.mergeCells(sheetId, "РОЗКЛАД", merges);

        return sheetId;
    }

    public static List<Pair<int[], int[]>> fillSchedule(Group group, List<List<Object>> data) {
        List<String> daysOfWeek = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
        List<Pair<int[], int[]>> merges = new ArrayList<>();

        Week firstWeek = group.weeks.get("firstWeek");
        for (Map.Entry<String, Day> day : firstWeek.days.entrySet()) {
            String dayOfWeek = day.getKey();
            if (dayOfWeek.equals("saturday")) {
                continue;
            }
            for (Map.Entry<String, Object> lesson : day.getValue().lessons.entrySet()) {
                String numberOfLesson = lesson.getKey();

                Map<String, Object> subjects = new HashMap<>();

                if (lesson.getValue() instanceof SubGroupPair) {
                    SubGroupPair<?, ?> pair = (SubGroupPair<?, ?>) lesson.getValue();

                    if (pair.getFirstGroup() != null) {
                        subjects.put("firstGroup", ((Subj) pair.getFirstGroup()).toHashMap());
                    } else {
                        subjects.put("firstGroup", null);
                    }

                    if (pair.getSecondGroup() != null) {
                        subjects.put("secondGroup", ((Subj) pair.getSecondGroup()).toHashMap());
                    } else {
                        subjects.put("secondGroup", null);
                    }

                } else if (lesson.getValue() instanceof Common) {
                    Common<?> common = (Common<?>) lesson.getValue();

                    if (common.getCommon() != null) {
                        subjects.put("common", ((Subj) common.getCommon()).toHashMap());
                    } else {
                        subjects.put("common", null);
                    }

                } else {
                    subjects = (Map<String, Object>) lesson.getValue();
                }

                if (subjects.containsKey("firstGroup") && subjects.containsKey("secondGroup")) {

                    LinkedHashMap<String, String> firstGroup = (LinkedHashMap<String, String>) subjects.get("firstGroup");
                    LinkedHashMap<String, String> secondGroup = (LinkedHashMap<String, String>) subjects.get("secondGroup");

                    String value;
                    if (firstGroup == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (firstGroup.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (firstGroup.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (firstGroup.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = firstGroup.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(firstGroup.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }
                    data.get(4 + Integer.parseInt(numberOfLesson) - 1).set(1 + daysOfWeek.indexOf(dayOfWeek) * 4, value);

                    if (secondGroup == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (secondGroup.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (secondGroup.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (secondGroup.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = secondGroup.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(secondGroup.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }
                    data.get(4 + Integer.parseInt(numberOfLesson) - 1).set(2 + daysOfWeek.indexOf(dayOfWeek) * 4, value);

                } else if (subjects.containsKey("common")) {
                    LinkedHashMap<String, String> common = (LinkedHashMap<String, String>) subjects.get("common");

                    String value;
                    if (common == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (common.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (common.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (common.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = common.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(common.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }
                    int row = 4 + Integer.parseInt(numberOfLesson) - 1;
                    int column = 1 + daysOfWeek.indexOf(dayOfWeek) * 4;
                    data.get(row).set(column, value);
                    data.get(row).set(column + 1, "");

                    merges.add(new Pair<>(new int[]{row, column}, new int[]{row, column + 1}));
                }
            }
        }

        Week secondWeek = group.weeks.get("secondWeek");
        for (Map.Entry<String, Day> day : secondWeek.days.entrySet()) {
            String dayOfWeek = day.getKey();
            if (dayOfWeek.equals("saturday")) {
                continue;
            }
            for (Map.Entry<String, Object> lesson : day.getValue().lessons.entrySet()) {
                String numberOfLesson = lesson.getKey();

                Map<String, Object> subjects = new HashMap<>();

                if (lesson.getValue() instanceof SubGroupPair) {
                    SubGroupPair<?, ?> pair = (SubGroupPair<?, ?>) lesson.getValue();

                    if (pair.getFirstGroup() != null) {
                        subjects.put("firstGroup", ((Subj) pair.getFirstGroup()).toHashMap());
                    } else {
                        subjects.put("firstGroup", null);
                    }

                    if (pair.getSecondGroup() != null) {
                        subjects.put("secondGroup", ((Subj) pair.getSecondGroup()).toHashMap());
                    } else {
                        subjects.put("secondGroup", null);
                    }

                } else if (lesson.getValue() instanceof Common) {
                    Common<?> common = (Common<?>) lesson.getValue();

                    if (common.getCommon() != null) {
                        subjects.put("common", ((Subj) common.getCommon()).toHashMap());
                    } else {
                        subjects.put("common", null);
                    }

                } else {
                    subjects = (Map<String, Object>) lesson.getValue();
                }

                if (subjects.containsKey("firstGroup") && subjects.containsKey("secondGroup")) {
                    LinkedHashMap<String, String> firstGroup = (LinkedHashMap<String, String>) subjects.get("firstGroup");
                    LinkedHashMap<String, String> secondGroup = (LinkedHashMap<String, String>) subjects.get("secondGroup");

                    String value;
                    if (firstGroup == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (firstGroup.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (firstGroup.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (firstGroup.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = firstGroup.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(firstGroup.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }
                    data.get(16 + Integer.parseInt(numberOfLesson) - 1).set(1 + daysOfWeek.indexOf(dayOfWeek) * 4, value);

                    if (secondGroup == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (secondGroup.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (secondGroup.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (secondGroup.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = secondGroup.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(secondGroup.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }
                    data.get(16 + Integer.parseInt(numberOfLesson) - 1).set(2 + daysOfWeek.indexOf(dayOfWeek) * 4, value);

                } else if (subjects.containsKey("common")) {
                    LinkedHashMap<String, String> common = (LinkedHashMap<String, String>) subjects.get("common");

                    String value;
                    if (common == null) {
                        value = "-";
                    } else {
                        value = "";
                        if (common.get("type").contains("Лаб")) {
                            value = "ЛР ";
                        } else if (common.get("type").contains("Практ")) {
                            value = "ПР ";
                        } else if (common.get("type").contains("Лекц")) {
                            value = "Лекція ";
                        }

                        String[] words = common.get("subject").split("\\s+");
                        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                        if (!subject.equals(common.get("subject"))) {
                            subject += "..";
                        }

                        value += subject;
                    }

                    int row = 16 + Integer.parseInt(numberOfLesson) - 1;
                    int column = 1 + daysOfWeek.indexOf(dayOfWeek) * 4;
                    data.get(row).set(column, value);
                    data.get(row).set(column + 1, "");

                    merges.add(new Pair<>(new int[]{row, column}, new int[]{row, column + 1}));
                }
            }
        }

        return merges;
    }

    public static void fillInfo(Group group, List<List<Object>> data) {
        LinkedHashMap<String, Subj> uniqSubject = new LinkedHashMap<>();


        LinkedHashSet<Day> mergedSet = new LinkedHashSet<>();

        mergedSet.addAll(group.weeks.get("firstWeek").getDays().values());
        mergedSet.addAll(group.weeks.get("secondWeek").getDays().values());

        for (Day day : mergedSet) {
            for (Map.Entry<String, Object> lesson : day.lessons.entrySet()) {

                Map<String, Object> subjects = new HashMap<>();

                if (lesson.getValue() instanceof SubGroupPair) {
                    subjects.put("firstGroup", ((SubGroupPair<?, ?>) lesson.getValue()).getFirstGroup());
                    subjects.put("secondGroup", ((SubGroupPair<?, ?>) lesson.getValue()).getSecondGroup());
                } else if (lesson.getValue() instanceof Common) {
                    subjects.put("common", ((Common<?>) lesson.getValue()).getCommon());
                } else {
                    subjects = (Map<String, Object>) lesson.getValue();
                }

                if (subjects.containsKey("firstGroup") && subjects.containsKey("secondGroup")) {

                    // Перша підгрупа
                    Subj firstSubj;
                    if (subjects.get("firstGroup") instanceof Subj) {
                        firstSubj = (Subj) subjects.get("firstGroup");
                    } else {
                        LinkedHashMap<String, String> firstGroup = (LinkedHashMap<String, String>) subjects.get("firstGroup");

                        if (firstGroup == null) {
                            firstSubj = null;
                        } else {
                            firstSubj = new Subj(firstGroup);
                        }
                    }

                    if (firstSubj != null) {
                        String firstKey = "firstGroup " + firstSubj.type + " " + firstSubj.subject;

                        if (uniqSubject.containsKey(firstKey)) {
                            Subj find = uniqSubject.get(firstKey);
                            if (find.auditory != null && firstSubj.auditory != null && !find.auditory.contains(firstSubj.auditory)) {
                                find.auditory += firstSubj.auditory + "/";
                            }
                        } else {
                            uniqSubject.put(firstKey, firstSubj);
                        }
                    }

                    // Друга підгрупа
                    Subj secondSubj;
                    if (subjects.get("secondGroup") instanceof Subj) {
                        secondSubj = (Subj) subjects.get("secondGroup");
                    } else {
                        LinkedHashMap<String, String> secondGroup = (LinkedHashMap<String, String>) subjects.get("secondGroup");

                        if (secondGroup == null) {
                            secondSubj = null;
                        } else {
                            secondSubj = new Subj(secondGroup);
                        }
                    }

                    if (secondSubj != null) {
                        String secondKey = "secondGroup " + secondSubj.type + " " + secondSubj.subject;

                        if (uniqSubject.containsKey(secondKey)) {
                            Subj find = uniqSubject.get(secondKey);
                            if (find.auditory != null && secondSubj.auditory != null && !find.auditory.contains(secondSubj.auditory)) {
                                find.auditory += "/" + secondSubj.auditory;
                            }
                        } else {
                            uniqSubject.put(secondKey, secondSubj);
                        }
                    }

                } else if (subjects.containsKey("common")) {
                    Subj commonSubj;
                    if (subjects.get("common") instanceof Subj) {
                        commonSubj = (Subj) subjects.get("common");
                    } else {
                        LinkedHashMap<String, String> common = (LinkedHashMap<String, String>) subjects.get("common");

                        if (common == null) {
                            commonSubj = null;
                        } else {
                            commonSubj = new Subj(common);
                        }
                    }

                    if (commonSubj != null) {
                        String commonKey = "common " + commonSubj.type + " " + commonSubj.subject;

                        if (uniqSubject.containsKey(commonKey)) {
                            Subj find = uniqSubject.get(commonKey);
                            if (find.auditory != null && commonSubj.auditory != null && !find.auditory.contains(commonSubj.auditory)) {
                                find.auditory += "/" + commonSubj.auditory;
                            }
                        } else {
                            uniqSubject.put(commonKey, commonSubj);
                        }
                    }
                }
            }
        }

        ArrayList<Map.Entry<String, Subj>> sortedUniq = new ArrayList<>(uniqSubject.entrySet());
        sortedUniq.sort(Comparator.comparing(o -> Arrays.stream(((Map.Entry<String, Subj>) o).getKey().split(" ")).skip(2).collect(Collectors.joining()))
                .thenComparing(a -> ((Map.Entry<String, Subj>) a).getKey().split(" ")[0]));

        SubjectInfo info = new SubjectInfo();

        int rowIndex = 2;
        for (Map.Entry<String, Subj> e : sortedUniq) {
            String[] keyWords = e.getKey().split(" ");

            switch (keyWords[0]) {
                case "common": {

                    info.splitOnSub = false;

                    String[] words = e.getValue().subject.split("\\s+");
                    info.name = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                    if (!info.name.equals(e.getValue().subject)) {
                        info.name += "..";
                    }

                    info.type = e.getValue().type;
                    info.commonAuditory = e.getValue().auditory;
                    info.commonLector = e.getValue().lector;

                    break;
                }
                case "firstGroup": {

                    info.splitOnSub = true;

                    String[] words = e.getValue().subject.split("\\s+");
                    info.name = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
                    if (!info.name.equals(e.getValue().subject)) {
                        info.name += "..";
                    }

                    info.type = e.getValue().type;

                    info.firstGroupAuditory = e.getValue().auditory;
                    info.firstGroupLector = e.getValue().lector;

                    break;
                }
                case "secondGroup": {

                    info.secondGroupAuditory = e.getValue().auditory;
                    info.secondGroupLector = e.getValue().lector;

                    break;
                }
            }

            if (keyWords[0].equals("common")) {
                String name = "";
                if (info.type != null) {
                    if (info.type.contains("Лаб")) {
                        name = "<b>ЛР</b> ";
                    } else if (info.type.contains("Практ")) {
                        name = "<b>ПР</b> ";
                    } else if (info.type.contains("Лекц")) {
                        name = "<b>Лекція</b> ";
                    }
                }
                name += info.name;

                data.get(rowIndex).set(0, name);
                data.get(rowIndex).set(1, name);
                data.get(rowIndex).set(4, info.commonLector == null ? "" : info.commonLector);
                data.get(rowIndex).set(5, info.commonAuditory == null ? "" : info.commonAuditory);
                rowIndex++;

                info = new SubjectInfo();
            } else if (keyWords[0].equals("secondGroup")) {
                String name = "";
                if (info.type != null) {
                    if (info.type.contains("Лаб")) {
                        name = "<b>ЛР</b> ";
                    } else if (info.type.contains("Практ")) {
                        name = "<b>ПР</b> ";
                    } else if (info.type.contains("Лекц")) {
                        name = "<b>Лекція</b> ";
                    }
                }
                name += info.name;

                data.get(rowIndex).set(0, name);
                data.get(rowIndex).set(1, name);
                data.get(rowIndex).set(8, info.firstGroupLector == null ? "" : info.firstGroupLector);
                data.get(rowIndex).set(9, info.firstGroupAuditory == null ? "" : info.firstGroupAuditory);
                data.get(rowIndex).set(12, info.secondGroupLector == null ? "" : info.secondGroupLector);
                data.get(rowIndex).set(13, info.secondGroupAuditory == null ? "" : info.secondGroupAuditory);
                rowIndex++;

                info = new SubjectInfo();
            }
        }
    }
}
