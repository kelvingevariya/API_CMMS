package com.grash.service;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.PredefinedAcl;
import com.google.cloud.storage.StorageOptions;
import com.grash.exception.CustomException;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class GCPService {
    @Value("${gcp.value}")
    private String gcpJson;
    @Value("${gcp.project-id}")
    private String gcpProjectId;
    @Value("${gcp.bucket-name}")
    private String gcpBucketName;

    public String upload(MultipartFile file, String folder) {
        Helper helper = new Helper();
        InputStream is = new ByteArrayInputStream(gcpJson.getBytes(StandardCharsets.UTF_8));
        Credentials credentials = null;
        try {
            credentials = GoogleCredentials
                    .fromStream(is);
        } catch (IOException e) {
            throw new CustomException("Wrong credentials", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                .setProjectId(gcpProjectId).build().getService();
        try {
            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder(gcpBucketName, folder + "/" + helper.generateString() + " " + file.getOriginalFilename()).build(), //get original file name
                    file.getBytes(), // the file
                    BlobTargetOption.predefinedAcl(PredefinedAcl.PUBLIC_READ) // Set file permission
            );
            return blobInfo.getMediaLink(); // Return file url
        } catch (IllegalStateException | IOException e) {
            throw new CustomException(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
