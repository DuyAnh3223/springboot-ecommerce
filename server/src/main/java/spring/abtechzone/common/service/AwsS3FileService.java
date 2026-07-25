package spring.abtechzone.common.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

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
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import spring.abtechzone.common.dto.AwsS3AccessUrlResponse;
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
    @Value("${aws.s3.public-folders}")
    String publicFoldersConfig;

    @NonFinal
    @Value("${aws.s3.presigned-url-expiration}")
    long defaultExpirationMinutes;

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
    Set<String> publicFolders;

    @NonFinal
    PrivateKey cachedPrivateKey;

    @PostConstruct
    void init() {
        if (publicFoldersConfig != null && !publicFoldersConfig.isBlank()) {
            this.publicFolders = Arrays.stream(publicFoldersConfig.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            this.publicFolders = Set.of("products", "categories", "avatars");
        }

        if (privateKeyContent != null && !privateKeyContent.isBlank()) {
            this.cachedPrivateKey = parsePrivateKey();
        }
    }

    /**
     * Check if folder/key belongs to a public folder prefix
     */
    public boolean isPublicFolder(String folderOrKey) {
        if (folderOrKey == null || folderOrKey.isBlank()) {
            return false;
        }

        String normalizedKey = folderOrKey.trim();
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }

        int slashIndex = normalizedKey.indexOf('/');
        String folderName = (slashIndex != -1) ? normalizedKey.substring(0, slashIndex) : normalizedKey;

        return publicFolders.contains(folderName.toLowerCase());
    }

    /**
     * Upload file to S3 at default folder (uploads/)
     */
    public AwsS3FileResponse upload(MultipartFile file) {
        return upload(file, "uploads");
    }

    /**
     * Upload file to S3 by folder (products, categories, avatars, etc.)
     */
    public AwsS3FileResponse upload(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String prefix = (folderName != null && !folderName.isBlank()) ? folderName.trim() + "/" : "";
        String fileKey = prefix + UUID.randomUUID() + "-" + sanitizedFilename;

        boolean isPublic = isPublicFolder(folderName);

        try {
            PutObjectRequest.Builder putBuilder =
                    PutObjectRequest.builder().bucket(bucket).key(fileKey).contentType(file.getContentType());

            if (isPublic) {
                putBuilder.cacheControl("public, max-age=31536000, immutable");
            }

            s3Client.putObject(putBuilder.build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl;
            if (isPublic) {
                fileUrl = buildPublicUrl(fileKey);
            } else {
                fileUrl = createPreSignedUrl(fileKey, Duration.ofMinutes(defaultExpirationMinutes));
            }

            log.info("File uploaded successfully to S3: key={}, bucket={}, isPublic={}", fileKey, bucket, isPublic);

            return AwsS3FileResponse.builder()
                    .fileName(sanitizedFilename)
                    .fileKey(fileKey)
                    .fileUrl(fileUrl)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .isPublic(isPublic)
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Get access URL response (public URL or CloudFront signed URL depending on key)
     */
    public AwsS3AccessUrlResponse getAccessUrl(String fileKey) {
        return getAccessUrl(fileKey, null);
    }

    /**
     * Get access URL response with custom TTL for private objects
     */
    public AwsS3AccessUrlResponse getAccessUrl(String fileKey, Long ttlMinutes) {
        boolean isPublic = isPublicFolder(fileKey);

        if (isPublic) {
            return AwsS3AccessUrlResponse.builder()
                    .url(buildPublicUrl(fileKey))
                    .isPublic(true)
                    .expiresAt(null)
                    .build();
        } else {
            long ttl = (ttlMinutes != null && ttlMinutes > 0) ? ttlMinutes : defaultExpirationMinutes;
            // Cap maximum TTL at 7 days (10080 minutes)
            ttl = Math.min(ttl, 10080);

            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(ttl));
            String url = createPreSignedUrl(fileKey, Duration.ofMinutes(ttl));

            return AwsS3AccessUrlResponse.builder()
                    .url(url)
                    .isPublic(false)
                    .expiresAt(expiresAt)
                    .build();
        }
    }

    /**
     * Get file's public URL on CloudFront
     *
     * @deprecated Prefer {@link #getAccessUrl(String)} to handle both public and private objects properly.
     */
    @Deprecated(since = "1.0.0")
    @SuppressWarnings("java:S1133")
    public String getFileUrl(String fileKey) {
        return buildPublicUrl(fileKey);
    }

    private String buildPublicUrl(String fileKey) {
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

    /**
     * Create CloudFront Signed URL for private object with duration
     */
    public String createPreSignedUrl(String keyName, Duration duration) {
        if (cloudfrontUrl == null || cloudfrontUrl.isBlank()) {
            log.error("CloudFront URL is not configured (cloudfront.url is required for CloudFront signed URLs)");
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        if (keyPairId == null || keyPairId.isBlank()) {
            log.error("CloudFront Key Pair ID is not configured (cloudfront.key-pair-id is required)");
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        PrivateKey privateKey = getPrivateKey();
        String resourceUrl = buildPublicUrl(keyName);
        Duration expiryDuration = (duration != null) ? duration : Duration.ofMinutes(defaultExpirationMinutes);
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
            log.error("Failed to generate CloudFront signed URL for key={}: {}", keyName, e.getMessage(), e);
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private PrivateKey getPrivateKey() {
        if (cachedPrivateKey == null) {
            cachedPrivateKey = parsePrivateKey();
        }
        return cachedPrivateKey;
    }

    /**
     * Parse CloudFront ECDSA (EC, prime256v1) Private Key from Base64 PKCS#8 string.
     */
    private PrivateKey parsePrivateKey() {
        if (privateKeyContent == null || privateKeyContent.isBlank()) {
            log.error("cloudfront.private-key is not configured");
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        String cleanedKey = privateKeyContent.replaceAll("\\s+", "");

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(cleanedKey);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 encoding in CloudFront private key: {}", e.getMessage());
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            log.info("Successfully loaded CloudFront ECDSA private key");
            return privateKey;
        } catch (GeneralSecurityException e) {
            log.error("Failed to parse CloudFront private key as PKCS#8 EC key: {}", e.getMessage());
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
