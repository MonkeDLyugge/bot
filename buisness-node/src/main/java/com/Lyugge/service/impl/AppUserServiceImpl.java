package com.Lyugge.service.impl;

import com.Lyugge.dao.AppUserDao;
import com.Lyugge.dto.MailParams;
import com.Lyugge.entity.AppUser;
import com.Lyugge.service.AppUserService;
import com.Lyugge.utils.CryptoTool;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static com.Lyugge.entity.enums.UserState.BASIC_STATE;
import static com.Lyugge.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;

@Service
@Log4j
public class AppUserServiceImpl implements AppUserService {
    private final AppUserDao appUserDao;
    private final CryptoTool cryptoTool;
    @Value("${service.mail.uri}")
    private String mailServiceUri;

    public AppUserServiceImpl(AppUserDao appUserDao, CryptoTool cryptoTool) {
        this.appUserDao = appUserDao;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()) {
            return "You've already registered.";
        } else if (appUser.getEmail() != null) {
            return "We've sent activation link to you, please check your email.";
        }
        appUser.setState(WAIT_FOR_EMAIL_STATE);
        appUserDao.save(appUser);
        return "Please enter your email.";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch(AddressException e) {
            return "Please enter correct email.";
        }
        var optionalAppUser = appUserDao.findByEmail(email);
        if (optionalAppUser.isEmpty()) {
            appUser.setState(BASIC_STATE);
            appUser.setEmail(email);
            appUser = appUserDao.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var message = String.format("Couldn't sent activation to email: %s.", email);
                log.error(message);
                appUser.setEmail(null);
                appUserDao.save(appUser);
                return message;
            }
            return "We've sent activation link to you, please check your email.";
        } else {
            return "This email is already in use. Please enter correct email.";
        }
    }

    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri,
                HttpMethod.POST,
                request,
                String.class);
    }
}
