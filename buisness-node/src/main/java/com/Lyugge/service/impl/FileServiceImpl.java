package com.Lyugge.service.impl;

import com.Lyugge.dao.AppDocumentDao;
import com.Lyugge.dao.AppPhotoDao;
import com.Lyugge.dao.BinaryContentDao;
import com.Lyugge.entity.AppDocument;
import com.Lyugge.entity.AppPhoto;
import com.Lyugge.entity.BinaryContent;
import com.Lyugge.exception.UploadFileException;
import com.Lyugge.service.FileService;
import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Log4j
@Service
@Component
public class FileServiceImpl implements FileService {
    @Value("${token}")
    private String token;
    @Value("${service.file_info.uri}")
    private String fileInfoUri;
    @Value("${service.file_storage.uri}")
    private String fileStorageUri;
    private final AppDocumentDao appDocumentDao;
    private final AppPhotoDao appPhotoDao;
    private final BinaryContentDao binaryContentDao;

    public FileServiceImpl(AppDocumentDao appDocumentDao, AppPhotoDao appPhotoDao, BinaryContentDao binaryContentDao) {
        this.appDocumentDao = appDocumentDao;
        this.appPhotoDao = appPhotoDao;
        this.binaryContentDao = binaryContentDao;
    }

    @Override
    public AppDocument processDoc(Message telegramMessage) {
        Document telegramDoc = telegramMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
            return appDocumentDao.save(transientAppDoc);
        } else {
            throw new UploadFileException("Bad response provided by telegram service: " + response);
        }
    }

    @Override
    public AppPhoto processPhoto(Message telegramMessage) {
        //TODO processing 1 photo now, should process all of them
        PhotoSize telegramPhoto = telegramMessage.getPhoto().get(0);
        String fileId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppPhoto transientAppPhoto = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
            return appPhotoDao.save(transientAppPhoto);
        } else {
            throw new UploadFileException("Bad response provided by telegram service: " + response);
        }
    }

    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileInByte = downloadFile(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileAsArrayOfBytes(fileInByte)
                .build();
        return binaryContentDao.save(transientBinaryContent);
    }

    private static String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return String.valueOf(jsonObject
                .getJSONObject("result")
                .getString("file_path"));
    }

    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
        return AppDocument.builder()
                .telegramFileId(telegramDoc.getFileId())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .fileSize(telegramDoc.getFileSize())
                .build();
    }

    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
        return AppPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .binaryContent(persistentBinaryContent)
                .fileSize(telegramPhoto.getFileSize())
                .build();
    }

    private byte[] downloadFile(String filePath) {
        String fullUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObject = null;
        try {
            urlObject = new URL(fullUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }

        //TODO try to optimize storage method
        try(InputStream is = urlObject.openStream()) {
            return is.readAllBytes();
        } catch(IOException e) {
            throw new UploadFileException(urlObject.toExternalForm(), e);
        }
    }

    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token,
                fileId
        );
    }
}
