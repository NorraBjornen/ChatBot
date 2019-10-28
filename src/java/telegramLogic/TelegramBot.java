package telegramLogic;

import datasource.FileReader;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class TelegramBot extends TelegramLongPollingBot {
    private static final String filename = "telegram_data";
    private static final String fileEncoding = "UTF-8";

    private String botName;
    private String botToken;

    private Queue<Message> inputMessages;

    TelegramBot(){
        inputMessages = new LinkedList<>();
        try (FileReader reader = new FileReader(filename, fileEncoding)) {
            botName = reader.readLine();
            botToken = reader.readLine();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update upd){
        inputMessages.add(upd.getMessage());
    }

    @Override
    public String getBotUsername(){
        return botName;
    }

    @Override
    public String getBotToken(){
        return botToken;
    }

    public void sendMsg(Long chatId, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(chatId);
        s.setText(text);
        try {
            execute(s);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public boolean isEmpty(){
        return inputMessages == null || inputMessages.isEmpty();
    }

    public Message removeInputMessage(){
        return inputMessages.remove();
    }
}
