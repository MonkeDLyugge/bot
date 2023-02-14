package com.Lyugge.service.impl;

import com.Lyugge.service.ConsumerService;
import com.Lyugge.service.MainService;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.Lyugge.model.RabbitQueue.*;


@Log4j
@Service
public class ConsumerServiceImpl implements ConsumerService {
    private final MainService mainService;

    public ConsumerServiceImpl(MainService mainService) {
        this.mainService = mainService;
    }

    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    @Override
    public void consumeTextMessageUpdates(Update update) {
        log.debug("NODE: Text message received now");
        mainService.processTextMessage(update);
    }

    @RabbitListener(queues = PHOTO_MESSAGE_UPDATE)
    @Override
    public void consumePhotoMessageUpdates(Update update) {
        log.debug("NODE: Photo message received now");
        mainService.processPhotoMessage(update);
    }

    @RabbitListener(queues = DOC_MESSAGE_UPDATE)
    @Override
    public void consumeDocMessageUpdates(Update update) {
        log.debug("NODE: Doc message received now");
        mainService.processDocMessage(update);
    }
}
