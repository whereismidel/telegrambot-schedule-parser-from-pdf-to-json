package com.midel;

import com.midel.pdf.LinkParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.InputStream;
import java.util.List;

import static com.midel.BotConfig.*;

public class ConvertPdfAndExcelTelegramBot extends TelegramLongPollingBot {

    static ConvertPdfAndExcelTelegramBot bot;

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update);

        Message message = update.getMessage();

        boolean hasMessage = update.hasMessage();
        boolean hasText = hasMessage && message.hasText();
        boolean hasDocument = hasMessage && message.hasDocument();
        boolean hasCaption = hasMessage && message.getCaption() != null;
        boolean isAdminMessage = hasMessage && message.getChatId().equals(CHAT_ADMIN);

        BotController botController = new BotController();

        String text = hasText ? message.getText() : "";
        String chatId = hasMessage ? message.getChatId().toString() : String.valueOf(CHAT_ADMIN);

        if (hasMessage && isAdminMessage) {
            if (hasDocument) {
                Document document = message.getDocument();
                String fileName = document.getFileName();

                File file = botController.getFileFromServer(document.getFileId());

                if (file != null) {
                    String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

                    try (InputStream pdfDocument = botController.getDocumentFromURL(fileUrl, fileName)) {
                        boolean onExport = (hasCaption && message.getCaption().equals("export")) || (hasText && text.contains("export"));
                        botController.getGroupTableResult(chatId, pdfDocument, onExport);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    botController.sendHTMLMessage(chatId, "File not found.");
                }

                return;

            }

            if (hasText && text.startsWith("https://")) {
                String fileUrl = message.getText().replace("export", "").trim();
                String[] split = fileUrl.split("/");
                String fileName = split[split.length - 1];

                try (InputStream pdfDocument = botController.getDocumentFromURL(fileUrl, fileName)) {
                    boolean onExport = text.contains("export");
                    botController.getGroupTableResult(chatId, pdfDocument, onExport);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }

            if (hasText && text.contains("download")) {
                List<String> links = LinkParser.parseLinks(PARSE_LINK_SOURCE, LINK_SEARCH_PATTERN);
                //System.out.println(links);
                boolean onExport = text.contains("export");

                for (String link : links) {
                    String[] split = link.split("/");
                    String fileName = split[split.length - 1];

                    try (InputStream pdfDocument = botController.getDocumentFromURL(link, fileName)) {
                        botController.getGroupTableResult(chatId, pdfDocument, onExport);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                return;
            }

            botController.sendHTMLMessage(chatId, "Надішли PDF документ або прямий URL на нього, якщо потрібен експорт в адмін таблицю, то додай слово export.");
        }
    }

    public static void main(String[] args) throws Exception {

        bot = new ConvertPdfAndExcelTelegramBot();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

//        for(com.google.api.services.drive.model.File file : DriveAPI.getFiles(true).getFiles()){
//           // if (!new Date(file.getModifiedTime().getValue()).after(new Date(new DateTime("2023-09-04T00:00:00.496Z").getValue()))) {
//                if (!file.getName().equals("Шаблон ФКПІ 444") && file.getName().equals("Шаблон")) {
//                    System.out.println(file.getName() + " " + file.getModifiedTime());
//                    //DriveAPI.deleteFile(file.getId());
//                }
//           // }
//        }
    }
}