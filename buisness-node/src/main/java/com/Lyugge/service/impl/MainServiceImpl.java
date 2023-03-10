package com.Lyugge.service.impl;

import com.Lyugge.dao.AppUserDao;
import com.Lyugge.dao.RawDataDao;
import com.Lyugge.entity.AppDocument;
import com.Lyugge.entity.AppPhoto;
import com.Lyugge.entity.AppUser;
import com.Lyugge.entity.RawData;
import com.Lyugge.exception.UploadFileException;
import com.Lyugge.service.AppUserService;
import com.Lyugge.service.FileService;
import com.Lyugge.service.MainService;
import com.Lyugge.service.ProducerService;
import com.Lyugge.service.enums.LinkType;
import com.Lyugge.service.enums.ServiceCommand;
import lombok.extern.log4j.Log4j;
import org.jvnet.hk2.annotations.Service;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.Lyugge.entity.enums.UserState.BASIC_STATE;
import static com.Lyugge.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static com.Lyugge.service.enums.ServiceCommand.*;

@Service
@Component
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDataDao rawDataDao;

    private final FileService fileService;
    private final ProducerService producerService;
    private final AppUserService appUserService;
    private final AppUserDao appUserDao;

    public MainServiceImpl(RawDataDao rawDataDao, FileService fileService, ProducerService producerService, AppUserService appUserService, AppUserDao appUserDao) {
        this.rawDataDao = rawDataDao;
        this.fileService = fileService;
        this.producerService = producerService;
        this.appUserService = appUserService;
        this.appUserDao = appUserDao;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);

        if (CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            output = appUserService.setEmail(appUser, text);
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
        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "Document have been uploaded successfully.\n" +
                    "Link: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            var error = "Unfortunately file have not been uploaded." +
                    "Please try again later..";
            sendAnswer(error, chatId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try {
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "Photo have been uploaded successfully.\n" +
                    "Link: " + link;
            sendAnswer(answer, chatId);
        } catch(UploadFileException e)  {
            log.error(e);
            var error = "Unfortunately file have not been uploaded." +
                    "Please try again later..";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Please sign up or login for uploading data";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Please stop right now running command. Type /cancel, then upload data";
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
        var serviceCommand = ServiceCommand.fromValue(cmd);
        if (REGISTRATION.equals(serviceCommand)) {
            return appUserService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Hello there! Wanna see the list of available commands? Type /help";
        } else {
            return "Command not found! Wanna see the list of available commands? Type /help";
        }
    }

    private String help() {
        return """
                List of available commands:
                /cancel - stopping command running right now;
                /registration - registration for a new users;
                """;
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDao.save(appUser);
        return "Command canceled!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        var message = update.getMessage();
        var telegramUser = message.getFrom();
        var optionalAppUser = appUserDao.findByTelegramUserId(telegramUser.getId());
        if (optionalAppUser.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserDao.save(transientAppUser);
        }
        return optionalAppUser.get();
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDao.save(rawData);
    }
}
