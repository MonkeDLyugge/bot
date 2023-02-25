package com.Lyugge.service;

import com.Lyugge.entity.AppDocument;
import com.Lyugge.entity.AppPhoto;
import com.Lyugge.service.enums.LinkType;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface FileService {
    AppDocument processDoc(Message telegramMessage);
    AppPhoto processPhoto(Message telegramMessage);
    String generateLink(Long id, LinkType linkType);
}
