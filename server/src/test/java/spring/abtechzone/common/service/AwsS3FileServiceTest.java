package spring.abtechzone.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import spring.abtechzone.common.dto.AwsS3AccessUrlResponse;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.exception.AppException;

@ExtendWith(MockitoExtension.class)
class AwsS3FileServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Utilities s3Utilities;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private AwsS3FileService awsS3FileService;

    private final String bucket = "test-bucket";

    @BeforeEach
    void setUp() throws Exception {
        awsS3FileService = new AwsS3FileService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(awsS3FileService, "bucket", bucket);
        ReflectionTestUtils.setField(awsS3FileService, "publicFoldersConfig", "products,categories,avatars");
        ReflectionTestUtils.setField(awsS3FileService, "defaultExpirationMinutes", 60L);
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

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/products/test.jpg").toURL());

        AwsS3FileResponse response = awsS3FileService.upload(file, "products");

        assertNotNull(response);
        assertEquals("test.jpg", response.getFileName());
        assertTrue(response.getFileKey().startsWith("products/"));
        assertEquals("image/jpeg", response.getContentType());
        assertTrue(response.isPublic());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_PrivateFolder_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "invoice.pdf", "application/pdf", "pdf content".getBytes());

        when(presignedGetObjectRequest.url())
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/documents/invoice.pdf?token=123")
                        .toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        AwsS3FileResponse response = awsS3FileService.upload(file, "documents");

        assertNotNull(response);
        assertEquals("invoice.pdf", response.getFileName());
        assertTrue(response.getFileKey().startsWith("documents/"));
        assertFalse(response.isPublic());
        assertTrue(response.getFileUrl().contains("token=123"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        assertThrows(AppException.class, () -> awsS3FileService.upload(emptyFile));
    }

    @Test
    void getAccessUrl_Public_Success() throws Exception {
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/products/test.jpg").toURL());

        AwsS3AccessUrlResponse response = awsS3FileService.getAccessUrl("products/test.jpg");

        assertNotNull(response);
        assertTrue(response.isPublic());
        assertNull(response.getExpiresAt());
        assertTrue(response.getUrl().contains("products/test.jpg"));
    }

    @Test
    void getAccessUrl_Private_Success() throws Exception {
        when(presignedGetObjectRequest.url())
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/documents/invoice.pdf?token=123")
                        .toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        AwsS3AccessUrlResponse response = awsS3FileService.getAccessUrl("documents/invoice.pdf", 30L);

        assertNotNull(response);
        assertFalse(response.isPublic());
        assertNotNull(response.getExpiresAt());
        assertTrue(response.getUrl().contains("token=123"));
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
