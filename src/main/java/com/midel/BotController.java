package com.midel;

import com.midel.google.DriveAPI;
import com.midel.google.SheetAPI;
import com.midel.group.Group;
import com.midel.group.GroupController;
import com.midel.pdf.TableController;
import org.javatuples.Pair;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.midel.BotConfig.ADMIN_INFO_SHEET;
import static com.midel.BotConfig.TEMPLATE_RANGE;
import static com.midel.google.CreateTable.createSheetFromGroup;

public class BotController extends ConvertPdfAndExcelTelegramBot {
    private static ConvertPdfAndExcelTelegramBot bot;

    public BotController() {
        if (bot == null) {
            bot = ConvertPdfAndExcelTelegramBot.bot;
        }
    }

    /**
     * <h3>Send message with HTML markup via telegram bot.</h3>
     *
     * @param chatId provided chatId in which messages would be sent.
     * @param text   provided message to be sent.
     */
    public int sendHTMLMessage(String chatId, String text) {
        org.telegram.telegrambots.meta.api.methods.send.SendMessage sendMessage = new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        try {
            return bot.execute(sendMessage).getMessageId(); // Надсилання
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * <h3>Send message with an attached document via the telegram bot.</h3>
     *
     * @param chatId       provided chatId in which messages would be sent.
     * @param document     provided document to be sent.
     * @param documentName provided documentName to be sent.
     */
    public int sendDocument(String chatId, InputStream document, String documentName) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);

        InputFile inputFile = new InputFile(document, documentName);
        sendDocument.setDocument(inputFile);

        try {
            return bot.execute(sendDocument).getMessageId(); // Надсилання
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * <h3>Get a File object by its file_id</h3>
     *
     * @param fileId provided the id of the file we want to get from telegram servers.
     */
    public File getFileFromServer(String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        File file;
        try {
            return execute(getFile);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InputStream getDocumentFromURL(String fileUrl, String fileName) throws Exception {
        InputStream inputStream = new URL(fileUrl).openStream();

        if (fileName.contains("xls") || fileName.contains("xlsx")) {
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) TableController.convertExcelToPDF(inputStream);

            inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        } else if (!fileName.contains("pdf")) {
            return null;
        }

        return inputStream;
    }

    public void getGroupTableResult(String chatId, InputStream pdfDocument, boolean onExport) {
        try {
            HashMap<String, Group> groups = GroupController.parseGroupsFromPDF(pdfDocument);

            for (Group group : groups
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(t -> t.group.split(" ")[0]))
                    .collect(Collectors.toList())) {

                Pair<OutputStream, String> jsonOutputFile = GroupController.groupToJson(group, true);

                if (jsonOutputFile != null && jsonOutputFile.getValue0() != null) {
                    InputStream jsonToSend = new ByteArrayInputStream(((ByteArrayOutputStream) jsonOutputFile.getValue0()).toByteArray());
                    sendDocument(chatId, jsonToSend, group.group + ".json");
                }

                if (onExport) {
                    List<List<Object>> existGroupsLinkData = SheetAPI.readSheetForRange(ADMIN_INFO_SHEET, TEMPLATE_RANGE);

                    if (existGroupsLinkData == null) {
                        return;
                    }

                    for (List<Object> list : existGroupsLinkData) {
                        if (list.size() == 2 && list.get(0).equals(group.group)) {
                            DriveAPI.deleteFile((String) list.get(1));
                            break;
                        }
                    }

                    String sheetId = createSheetFromGroup(group, existGroupsLinkData);

                    sendHTMLMessage(chatId, "https://docs.google.com/spreadsheets/d/" + sheetId);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
