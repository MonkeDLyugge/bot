package com.Lyugge.service.controller;


import com.Lyugge.service.FileService;
import lombok.extern.log4j.Log4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j
@RequestMapping("/file")
@RestController
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequestMapping(value = "/get_doc", method = RequestMethod.GET)
    public ResponseEntity<?> getDoc(@RequestParam("id") String id) {
        //TODO Add ControllerAdvice to creating badRequest description
        var doc = fileService.getDocument(id);
        if (doc == null) {
            return ResponseEntity.badRequest().build();
        }
        var binaryContentId = doc.getBinaryContent();

        var fileSystemResource =  fileService.getFileSystemResource(binaryContentId);
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .header("Content-disposition", "attachment; filename=" + doc.getDocName())
                .body(fileSystemResource);
    }

    @RequestMapping(value = "/get_photo", method = RequestMethod.GET)
    public ResponseEntity<?> getPhoto(@RequestParam("id") String id) {
        //TODO Add ControllerAdvice to creating badRequest description
        var photo = fileService.getPhoto(id);

        if (photo == null) {
            return ResponseEntity.badRequest().build();
        }

        var binaryContentId = photo.getBinaryContent();

        var fileSystemResource =  fileService.getFileSystemResource(binaryContentId);
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header("Content-disposition", "attachment;")
                .body(fileSystemResource);
    }
}
