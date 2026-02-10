package com.app.service;

import com.app.config.AppProps;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 스토리지 서비스
 * 
 * S3에 파일 업로드, 삭제, Presigned URL 생성 기능을 제공합니다.
 */
@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProps appProps;
    private final S3Utilities s3Utilities;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, AppProps appProps) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.appProps = appProps;
        this.s3Utilities = s3Client.utilities();
    }

    public UploadResult upload(String directory, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        AppProps.Aws.S3 props = s3Props();
        String key = buildObjectKey(directory, file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        return new UploadResult(key, resolvePublicUrl(key));
    }

    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }

        AppProps.Aws.S3 props = s3Props();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build());
    }

    public URL createPresignedGetUrl(String key, Duration ttl) {
        AppProps.Aws.S3 props = s3Props();
        Duration duration = ttl != null ? ttl : Duration.ofMinutes(5);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(builder -> builder
                        .bucket(props.getBucket())
                        .key(key))
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url();
    }

    private String buildObjectKey(String directory, String originalFilename) {
        String cleanDirectory = StringUtils.hasText(directory) ? directory.trim() : "";
        String extension = "";

        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String generatedName = UUID.randomUUID().toString().replace("-", "");
        String key = generatedName + extension;

        if (StringUtils.hasText(cleanDirectory)) {
            key = cleanDirectory + "/" + key;
        }

        return key;
    }

    private String resolvePublicUrl(String key) {
        AppProps.Aws.S3 props = s3Props();
        if (StringUtils.hasText(props.getBaseUrl())) {
            return props.getBaseUrl().replaceAll("/$", "") + "/" + key;
        }

        return s3Utilities.getUrl(builder -> builder
                .bucket(props.getBucket())
                .key(key))
                .toExternalForm();
    }

    private AppProps.Aws.S3 s3Props() {
        return appProps.getAws().getS3();
    }

    public record UploadResult(String key, String url) { }
}
