package com.Lyugge.service.impl;

import com.Lyugge.dao.AppUserDao;
import com.Lyugge.dao.RawDataDao;
import com.Lyugge.entity.AppUser;
import com.Lyugge.entity.RawData;
import com.Lyugge.entity.enums.UserState;
import com.Lyugge.service.MainService;
import com.Lyugge.service.ProducerService;
import lombok.extern.log4j.Log4j;
import org.jvnet.hk2.annotations.Service;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.Lyugge.entity.enums.UserState.BASIC_STATE;
import static com.Lyugge.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static com.Lyugge.service.enums.ServiceCommands.*;

@Service
@Component
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDataDao rawDataDao;
    private final ProducerService producerService;
    private final AppUserDao appUserDao;

    public MainServiceImpl(RawDataDao rawDataDao, ProducerService producerService, AppUserDao appUserDao) {
        this.rawDataDao = rawDataDao;
        this.producerService = producerService;
        this.appUserDao = appUserDao;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        var message = update.getMessage();
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        if (CANCEL.equals(text)) {
            output = CancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            //TODO add support for this branch
        } else {
            log.error("Unknown user state: " + userState);
            output = "Unknown error! Try /cancel and start again!";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        //TODO add saving document
        var answer = "Document successfully uploaded! Here is your link): https://test_link/doc";
        sendAnswer(answer, chatId);
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        //TODO add saving photo
        var answer = "Photo successfully uploaded! Here is your link): https://test_link/photo";
        sendAnswer(answer, chatId);
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Please sign up or login for uploading data";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Please stop right now running command. Type /cancel for uploading data";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }


    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        if (REGISTRATION.equals(cmd)) {
            //TODO add registration
            return "Command not working now..";
        } else if (HELP.equals(cmd)) {
            return help();
        } else if (START.equals(cmd)) {
            return "Hello there! Wanna see the list of available commands? Type /help";
        } else {
            return "Command not found! Wanna see the list of available commands? Type /help";
        }
    }

    private String help() {
        return "List of available commands:\n" +
                "/cancel - stopping command running right now;\n" +
                "/registration - registration for a new users;\n";
    }

    private String CancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDao.save(appUser);
        return "Command canceled!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        var message = update.getMessage();
        var telegramUser = message.getFrom();
        AppUser persistentAppUser = appUserDao.findAppUserByTelegramUserId(telegramUser.getId());
        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    //TODO change isActive field
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDao.save(transientAppUser);
        }
        return persistentAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDao.save(rawData);
    }
}
