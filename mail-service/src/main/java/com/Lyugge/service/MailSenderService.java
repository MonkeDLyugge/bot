package com.Lyugge.service;

import com.Lyugge.dto.MailParams;

public interface MailSenderService {
    void send(MailParams mailParams);
}
