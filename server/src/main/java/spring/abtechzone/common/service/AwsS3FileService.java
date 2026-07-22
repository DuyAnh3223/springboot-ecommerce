package spring.abtechzone.common.service;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AwsS3FileService {

    S3Client s3Client;

    @NonFinal
    @Value("${aws.s3.bucket}")
    String bucket;

    /**
     * Upload file to S3 at default folder (uploads/)
     */
    public AwsS3FileResponse upload(MultipartFile file) {
        return upload(file, "uploads");
    }

    /**
     * Upload file to S3 by folder ( products, categories, avatars)
     */
    public AwsS3FileResponse upload(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String prefix = (folderName != null && !folderName.isBlank()) ? folderName.trim() + "/" : "";
        String fileKey = prefix + UUID.randomUUID() + "-" + sanitizedFilename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = getFileUrl(fileKey);

            log.info("File uploaded successfully to S3: key={}, bucket={}", fileKey, bucket);

            return AwsS3FileResponse.builder()
                    .fileName(sanitizedFilename)
                    .fileKey(fileKey)
                    .fileUrl(fileUrl)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Get file's pulbic URL on S3
     */
    public String getFileUrl(String fileKey) {
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(fileKey).build())
                .toExternalForm();
    }

    /**
     * GetObject file on S3 under byte array
     */
    public ResponseBytes<GetObjectResponse> getObject(String fileKey) {
        try {
            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder().bucket(bucket).key(fileKey).build();

            return s3Client.getObjectAsBytes(getObjectRequest);
        } catch (Exception e) {
            log.error("Failed to get object from S3 for key={}: {}", fileKey, e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * DeleteObject file from S3
     */
    public void deleteObject(String fileKey) {
        try {
            DeleteObjectRequest deleteObjectRequest =
                    DeleteObjectRequest.builder().bucket(bucket).key(fileKey).build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: key={}, bucket={}", fileKey, bucket);
        } catch (Exception e) {
            log.error("Failed to delete object from S3 for key={}: {}", fileKey, e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
