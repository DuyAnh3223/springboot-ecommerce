package spring.abtechzone.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.exception.AppException;

@ExtendWith(MockitoExtension.class)
class AwsS3FileServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Utilities s3Utilities;

    private AwsS3FileService awsS3FileService;

    private final String bucket = "test-bucket";

    @BeforeEach
    void setUp() {
        awsS3FileService = new AwsS3FileService(s3Client);
        ReflectionTestUtils.setField(awsS3FileService, "bucket", bucket);
    }

    @Test
    void upload_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class)))
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/uploads/test.jpg").toURL());

        AwsS3FileResponse response = awsS3FileService.upload(file, "products");

        assertNotNull(response);
        assertEquals("test.jpg", response.getFileName());
        assertTrue(response.getFileKey().startsWith("products/"));
        assertEquals("image/jpeg", response.getContentType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        assertThrows(AppException.class, () -> awsS3FileService.upload(emptyFile));
    }

    @Test
    void getFileUrl_Success() throws Exception {
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class)))
                .thenReturn(new java.net.URI("https://test-bucket.s3.amazonaws.com/products/test.jpg").toURL());

        String url = awsS3FileService.getFileUrl("products/test.jpg");

        assertNotNull(url);
        assertTrue(url.contains("test.jpg"));
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
