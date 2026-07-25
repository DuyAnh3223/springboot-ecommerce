package spring.abtechzone.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import spring.abtechzone.common.dto.AwsS3AccessUrlResponse;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.exception.AppException;

@ExtendWith(MockitoExtension.class)
class AwsS3FileServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private CloudFrontUtilities cloudFrontUtilities;

    private AwsS3FileService awsS3FileService;

    private final String bucket = "test-bucket";
    private String ecPrivateKeyBase64;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();
        byte[] encoded = keyPair.getPrivate().getEncoded();
        ecPrivateKeyBase64 = Base64.getEncoder().encodeToString(encoded);

        awsS3FileService = new AwsS3FileService(s3Client, cloudFrontUtilities);
        ReflectionTestUtils.setField(awsS3FileService, "bucket", bucket);
        ReflectionTestUtils.setField(awsS3FileService, "publicFoldersConfig", "products,categories,avatars");
        ReflectionTestUtils.setField(awsS3FileService, "defaultExpirationMinutes", 60L);
        ReflectionTestUtils.setField(awsS3FileService, "cloudfrontUrl", "https://d111111abcdef8.cloudfront.net");
        ReflectionTestUtils.setField(awsS3FileService, "keyPairId", "K2JC0ABCDEFG123");
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", ecPrivateKeyBase64);
        ReflectionTestUtils.invokeMethod(awsS3FileService, "init");
    }

    @Test
    void isPublicFolder_Tests() {
        assertTrue(awsS3FileService.isPublicFolder("products"));
        assertTrue(awsS3FileService.isPublicFolder("products/subfolder/file.jpg"));
        assertTrue(awsS3FileService.isPublicFolder("/categories/cat.png"));
        assertFalse(awsS3FileService.isPublicFolder("documents/secret.pdf"));
        assertFalse(awsS3FileService.isPublicFolder("documentsX/secret.pdf"));
        assertFalse(awsS3FileService.isPublicFolder(null));
        assertFalse(awsS3FileService.isPublicFolder(""));
    }

    @Test
    void upload_PublicFolder_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());

        AwsS3FileResponse response = awsS3FileService.upload(file, "products");

        assertNotNull(response);
        assertEquals("test.jpg", response.getFileName());
        assertTrue(response.getFileKey().startsWith("products/"));
        assertEquals("image/jpeg", response.getContentType());
        assertTrue(response.isPublic());
        assertTrue(response.getFileUrl().startsWith("https://d111111abcdef8.cloudfront.net/products/"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_PrivateFolder_CloudFrontSignedUrl_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf content".getBytes());

        String signedUrlString =
                "https://d111111abcdef8.cloudfront.net/documents/invoice.pdf?Key-Pair-Id=K2JC0ABCDEFG123&Signature=xyz&Expires=9999";
        SignedUrl signedUrlMock = mock(SignedUrl.class);
        when(signedUrlMock.url()).thenReturn(signedUrlString);
        when(cloudFrontUtilities.getSignedUrlWithCannedPolicy(any(CannedSignerRequest.class)))
                .thenReturn(signedUrlMock);

        AwsS3FileResponse response = awsS3FileService.upload(file, "documents");

        assertNotNull(response);
        assertEquals("invoice.pdf", response.getFileName());
        assertTrue(response.getFileKey().startsWith("documents/"));
        assertFalse(response.isPublic());
        assertTrue(response.getFileUrl().contains("Key-Pair-Id=K2JC0ABCDEFG123"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        assertThrows(AppException.class, () -> awsS3FileService.upload(emptyFile));
    }

    @Test
    void getAccessUrl_Public_Success() throws Exception {
        AwsS3AccessUrlResponse response = awsS3FileService.getAccessUrl("products/test.jpg");

        assertNotNull(response);
        assertTrue(response.isPublic());
        assertNull(response.getExpiresAt());
        assertEquals("https://d111111abcdef8.cloudfront.net/products/test.jpg", response.getUrl());
    }

    @Test
    void getAccessUrl_Private_CloudFrontECDSA_Success() throws Exception {
        String expectedSignedUrl =
                "https://d111111abcdef8.cloudfront.net/documents/invoice.pdf?Key-Pair-Id=K2JC0ABCDEFG123&Signature=abc123sig&Expires=1234567";
        SignedUrl signedUrlMock = mock(SignedUrl.class);
        when(signedUrlMock.url()).thenReturn(expectedSignedUrl);

        ArgumentCaptor<CannedSignerRequest> requestCaptor = ArgumentCaptor.forClass(CannedSignerRequest.class);
        when(cloudFrontUtilities.getSignedUrlWithCannedPolicy(requestCaptor.capture()))
                .thenReturn(signedUrlMock);

        AwsS3AccessUrlResponse response = awsS3FileService.getAccessUrl("documents/invoice.pdf", 30L);

        assertNotNull(response);
        assertFalse(response.isPublic());
        assertNotNull(response.getExpiresAt());
        assertEquals(expectedSignedUrl, response.getUrl());

        CannedSignerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("K2JC0ABCDEFG123", capturedRequest.keyPairId());
        assertEquals("EC", capturedRequest.privateKey().getAlgorithm());
    }

    @Test
    void getAccessUrl_Private_MissingKeyPairId_ThrowsException() {
        ReflectionTestUtils.setField(awsS3FileService, "keyPairId", "");

        assertThrows(AppException.class, () -> awsS3FileService.getAccessUrl("documents/invoice.pdf", 30L));
    }

    @Test
    void getAccessUrl_Private_InvalidPrivateKey_ThrowsException() {
        ReflectionTestUtils.setField(awsS3FileService, "cachedPrivateKey", null);
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", "INVALID_BASE64_$%#");

        assertThrows(AppException.class, () -> awsS3FileService.getAccessUrl("documents/invoice.pdf", 30L));
    }

    @Test
    void getAccessUrl_Private_RsaPrivateKey_ThrowsException() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        String rsaPrivateKeyBase64 =
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        ReflectionTestUtils.setField(awsS3FileService, "cachedPrivateKey", null);
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", rsaPrivateKeyBase64);

        assertThrows(AppException.class, () -> awsS3FileService.getAccessUrl("documents/invoice.pdf", 30L));
    }

    @Test
    void getObject_Success() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .contentLength(10L)
                .build();
        ResponseBytes<GetObjectResponse> responseBytes =
                ResponseBytes.fromByteArray(getObjectResponse, "test data".getBytes());

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        ResponseBytes<GetObjectResponse> result = awsS3FileService.getObject("products/test.jpg");

        assertNotNull(result);
        assertEquals(10L, result.response().contentLength());
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void deleteObject_Success() {
        doReturn(DeleteObjectResponse.builder().build()).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> awsS3FileService.deleteObject("products/test.jpg"));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void getAccessUrl_PublicFolder_CloudFront_Success() {
        ReflectionTestUtils.setField(awsS3FileService, "cloudfrontUrl", "https://d111111abcdef8.cloudfront.net/");

        AwsS3AccessUrlResponse response = awsS3FileService.getAccessUrl("products/item-1.png");

        assertNotNull(response);
        assertTrue(response.isPublic());
        assertNull(response.getExpiresAt());
        assertEquals("https://d111111abcdef8.cloudfront.net/products/item-1.png", response.getUrl());
    }

    @Test
    void getAccessUrl_ThrowsException_WhenCloudFrontUrlMissing() {
        ReflectionTestUtils.setField(awsS3FileService, "cloudfrontUrl", "");

        assertThrows(AppException.class, () -> awsS3FileService.getAccessUrl("products/item-1.png"));
    }
}
