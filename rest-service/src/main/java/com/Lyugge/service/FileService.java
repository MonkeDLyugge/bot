package com.Lyugge.service;

import com.Lyugge.entity.AppDocument;
import com.Lyugge.entity.AppPhoto;
import com.Lyugge.entity.BinaryContent;
import org.springframework.core.io.FileSystemResource;

public interface FileService {
    AppDocument getDocument(String id);
    AppPhoto getPhoto(String id);
    FileSystemResource getFileSystemResource(BinaryContent binaryContent);
}
