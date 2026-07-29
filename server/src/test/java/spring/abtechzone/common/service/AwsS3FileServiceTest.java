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
    private String rsaPrivateKeyBase64;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator ecKpg = KeyPairGenerator.getInstance("EC");
        ecKpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair ecKeyPair = ecKpg.generateKeyPair();
        ecPrivateKeyBase64 =
                Base64.getEncoder().encodeToString(ecKeyPair.getPrivate().getEncoded());

        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKeyPair = rsaKpg.generateKeyPair();
        rsaPrivateKeyBase64 =
                Base64.getEncoder().encodeToString(rsaKeyPair.getPrivate().getEncoded());

        awsS3FileService = new AwsS3FileService(s3Client, cloudFrontUtilities);
        ReflectionTestUtils.setField(awsS3FileService, "bucket", bucket);
        ReflectionTestUtils.setField(awsS3FileService, "cloudfrontUrl", "https://d111111abcdef8.cloudfront.net");
        ReflectionTestUtils.setField(awsS3FileService, "keyPairId", "K2JC0ABCDEFG123");
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", ecPrivateKeyBase64);
        ReflectionTestUtils.invokeMethod(awsS3FileService, "init");
    }

    @Test
    void parsePrivateKey_SupportsECAndRSA() throws Exception {
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", ecPrivateKeyBase64);
        Object ecKey = ReflectionTestUtils.invokeMethod(awsS3FileService, "parsePrivateKey");
        assertNotNull(ecKey);

        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", rsaPrivateKeyBase64);
        Object rsaKey = ReflectionTestUtils.invokeMethod(awsS3FileService, "parsePrivateKey");
        assertNotNull(rsaKey);
    }

    @Test
    void extractS3Key_Tests() {
        assertEquals("categories/cat1.png", awsS3FileService.extractS3Key("categories/cat1.png"));
        assertEquals(
                "categories/cat1.png",
                awsS3FileService.extractS3Key(
                        "https://d111111abcdef8.cloudfront.net/categories/cat1.png?Expires=123&Signature=abc"));
        assertNull(awsS3FileService.extractS3Key(null));
        assertNull(awsS3FileService.extractS3Key("   "));
        assertNull(awsS3FileService.extractS3Key("https://external-domain.com/image.png"));
    }

    @Test
    void resolveAccessUrl_ExternalUrl_ReturnsNull() {
        assertNull(awsS3FileService.resolveAccessUrl("https://external-domain.com/image.png"));
    }

    @Test
    void resolveAccessUrl_WithPrivateKey_ReturnsSignedUrl() throws Exception {
        String expectedSignedUrl =
                "https://d111111abcdef8.cloudfront.net/categories/cat1.png?Key-Pair-Id=K2JC0ABCDEFG123&Signature=sig";
        SignedUrl signedUrlMock = mock(SignedUrl.class);
        when(signedUrlMock.url()).thenReturn(expectedSignedUrl);
        when(cloudFrontUtilities.getSignedUrlWithCannedPolicy(any(CannedSignerRequest.class)))
                .thenReturn(signedUrlMock);

        String result = awsS3FileService.resolveAccessUrl("categories/cat1.png");

        assertNotNull(result);
        assertEquals(expectedSignedUrl, result);
    }

    @Test
    void resolveAccessUrl_MissingPrivateKey_ReturnsNull() {
        ReflectionTestUtils.setField(awsS3FileService, "cachedPrivateKey", null);
        ReflectionTestUtils.setField(awsS3FileService, "privateKeyContent", "");

        String result = awsS3FileService.resolveAccessUrl("categories/cat1.png");

        assertNull(result);
    }

    @Test
    void resolveAccessUrl_ExceptionOccurs_ReturnsNull() throws Exception {
        when(cloudFrontUtilities.getSignedUrlWithCannedPolicy(any(CannedSignerRequest.class)))
                .thenThrow(new RuntimeException("Signing failure"));

        String result = awsS3FileService.resolveAccessUrl("categories/cat1.png");

        assertNull(result);
    }

    @Test
    void upload_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());

        AwsS3FileResponse response = awsS3FileService.upload(file, "products");

        assertNotNull(response);
        assertTrue(response.getFileKey().startsWith("products/"));
        assertEquals("image/jpeg", response.getContentType());
        assertTrue(response.isPublic());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        assertThrows(AppException.class, () -> awsS3FileService.upload(emptyFile, "uploads"));
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
}
