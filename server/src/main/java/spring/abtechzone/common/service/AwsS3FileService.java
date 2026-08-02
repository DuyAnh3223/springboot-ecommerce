package spring.abtechzone.common.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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
    CloudFrontUtilities cloudFrontUtilities;

    @NonFinal
    @Value("${aws.s3.bucket}")
    String bucket;

    @NonFinal
    @Value("${cloudfront.url:}")
    String cloudfrontUrl;

    @NonFinal
    @Value("${cloudfront.key-pair-id:}")
    String keyPairId;

    @NonFinal
    @Value("${cloudfront.private-key:}")
    String privateKeyContent;

    @NonFinal
    @Value("${cloudfront.signed-url-expiration-days:7}")
    long signedUrlExpirationDays;

    @NonFinal
    PrivateKey cachedPrivateKey;

    @PostConstruct
    void init() {
        if (privateKeyContent != null && !privateKeyContent.isBlank()) {
            this.cachedPrivateKey = parsePrivateKey();
        }
    }

    /**
     * Extract raw S3 file key from a key string or full URL (handling domain prefix and query params)
     */
    @PreAuthorize("permitAll()")
    public String extractS3Key(String thumbnailOrUrl) {
        if (thumbnailOrUrl == null || thumbnailOrUrl.isBlank()) {
            return null;
        }

        String key = thumbnailOrUrl.trim();

        if (key.startsWith("http://") || key.startsWith("https://")) {
            boolean isOurDomain = (cloudfrontUrl != null && !cloudfrontUrl.isBlank() && key.contains(cloudfrontUrl))
                    || (bucket != null && !bucket.isBlank() && key.contains(bucket));
            if (isOurDomain) {
                try {
                    int pathStart = key.indexOf('/', key.indexOf("://") + 3);
                    if (pathStart != -1) {
                        key = key.substring(pathStart + 1);
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract S3 key from URL={}: {}", thumbnailOrUrl, e.getMessage());
                }
            } else {
                return null;
            }
        }

        int queryIdx = key.indexOf('?');
        if (queryIdx != -1) {
            key = key.substring(0, queryIdx);
        }

        while (key.startsWith("/")) {
            key = key.substring(1);
        }

        if (key.isBlank()) {
            log.warn("Extracted S3 key is blank for input={}", thumbnailOrUrl);
            return null;
        }

        return key;
    }

    /**
     * Dynamically resolve access URL (Signed URL via CloudFront)
     */
    @PreAuthorize("permitAll()")
    public String resolveAccessUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }

        String s3Key = extractS3Key(keyOrUrl);
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        if (cachedPrivateKey == null || keyPairId == null || keyPairId.isBlank()) {
            log.error("CloudFront private key or Key Pair ID is missing. Cannot sign access URL for key={}", s3Key);
            return null;
        }

        try {
            return createCloudFrontSignedUrl(s3Key, Duration.ofDays(signedUrlExpirationDays));
        } catch (Exception e) {
            log.error("Failed to create CloudFront signed URL for key={}: {}", s3Key, e.getMessage(), e);
            return null;
        }
    }

    /** Upload a file to {@code folderName/<uuid>}. */
    @PreAuthorize("isAuthenticated()")
    public AwsS3FileResponse upload(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (folderName == null || !folderName.matches("[A-Za-z0-9_-]+")) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        String fileKey = folderName + "/" + UUID.randomUUID();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = resolveAccessUrl(fileKey);

            log.info("File uploaded successfully to S3: key={}, bucket={}", fileKey, bucket);

            return AwsS3FileResponse.builder()
                    .fileKey(fileKey)
                    .fileUrl(fileUrl)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .isPublic(true)
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @PreAuthorize("permitAll()")
    public String buildUrl(String fileKey) {
        if (cloudfrontUrl == null || cloudfrontUrl.isBlank()) {
            log.error("CloudFront URL is not configured (cloudfront.url is required for public assets)");
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        String baseUrl = cloudfrontUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String key = fileKey != null ? fileKey.trim() : "";
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return baseUrl + "/" + key;
    }

    /**
     * Create CloudFront Signed URL for object with duration
     */
    @PreAuthorize("permitAll()")
    public String createCloudFrontSignedUrl(String s3Key, Duration validity) {
        if (cloudfrontUrl == null || cloudfrontUrl.isBlank()) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        if (keyPairId == null || keyPairId.isBlank()) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        PrivateKey privateKey = getPrivateKey();
        if (privateKey == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        String resourceUrl = buildUrl(s3Key);
        Duration expiryDuration = (validity != null) ? validity : Duration.ofDays(signedUrlExpirationDays);
        Instant expirationDate = Instant.now().plus(expiryDuration);

        try {
            CannedSignerRequest cannedSignerRequest = CannedSignerRequest.builder()
                    .resourceUrl(resourceUrl)
                    .keyPairId(keyPairId.trim())
                    .privateKey(privateKey)
                    .expirationDate(expirationDate)
                    .build();

            SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(cannedSignerRequest);
            return signedUrl.url();
        } catch (Exception e) {
            log.error("Failed to generate CloudFront signed URL for key={}: {}", s3Key, e.getMessage(), e);
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * GetObject file on S3 under byte array
     */
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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

    private PrivateKey getPrivateKey() {
        if (cachedPrivateKey == null) {
            cachedPrivateKey = parsePrivateKey();
        }
        return cachedPrivateKey;
    }

    /**
     * Parse CloudFront Private Key from Base64 PKCS#8 string (supports RSA and EC)
     */
    private PrivateKey parsePrivateKey() {
        if (privateKeyContent == null || privateKeyContent.isBlank()) {
            return null;
        }

        String cleanedKey = privateKeyContent.replaceAll("\\s+", "");

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(cleanedKey);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 encoding in CloudFront private key: {}", e.getMessage());
            return null;
        }

        for (String algo : new String[] {"RSA", "EC"}) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algo);
                PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
                log.info("Successfully loaded CloudFront {} private key", algo);
                return privateKey;
            } catch (GeneralSecurityException ignored) {
                // Try next algorithm
            }
        }

        log.error("Failed to parse CloudFront private key as RSA or EC PKCS#8 key");
        return null;
    }
}
