package com.talentflow.storage;

import com.talentflow.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final StorageProperties props;

    @Override
    public void upload(String key, byte[] content, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(props.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException ex) {
            throw new StorageException("Failed to store file", ex);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build()).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("File not found");
        } catch (SdkException ex) {
            throw new StorageException("Failed to read file", ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build());
        } catch (SdkException ex) {
            throw new StorageException("Failed to delete file", ex);
        }
    }
}
