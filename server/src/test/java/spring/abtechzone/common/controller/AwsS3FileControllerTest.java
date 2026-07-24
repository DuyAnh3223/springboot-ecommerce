package spring.abtechzone.common.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import spring.abtechzone.common.dto.AwsS3AccessUrlResponse;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.common.exception.GlobalExceptionHandler;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AwsS3FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AwsS3FileService awsS3FileService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AwsS3FileController awsS3FileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(awsS3FileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadFile_Success() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy image content".getBytes());

        AwsS3FileResponse awsS3FileResponse = AwsS3FileResponse.builder()
                .fileName("test.jpg")
                .fileKey("uploads/test.jpg")
                .fileUrl("https://bucket.s3.amazonaws.com/uploads/test.jpg")
                .contentType("image/jpeg")
                .size(19L)
                .build();

        when(awsS3FileService.upload(any(), eq("uploads"))).thenReturn(awsS3FileResponse);

        mockMvc.perform(multipart("/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fileName").value("test.jpg"))
                .andExpect(jsonPath("$.result.fileKey").value("uploads/test.jpg"));
    }

    @Test
    void getAccessUrl_PublicFolder_Success() throws Exception {
        AwsS3AccessUrlResponse response = AwsS3AccessUrlResponse.builder()
                .url("https://bucket.s3.amazonaws.com/products/test.jpg")
                .isPublic(true)
                .expiresAt(null)
                .build();

        when(awsS3FileService.isPublicFolder("products/test.jpg")).thenReturn(true);
        when(awsS3FileService.getAccessUrl("products/test.jpg", null)).thenReturn(response);

        mockMvc.perform(get("/files/access-url").param("key", "products/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.url").value("https://bucket.s3.amazonaws.com/products/test.jpg"))
                .andExpect(jsonPath("$.result.public").value(true));
    }

    @Test
    void getAccessUrl_PrivateFolder_Unauthenticated_ThrowsException() throws Exception {
        when(awsS3FileService.isPublicFolder("documents/doc.pdf")).thenReturn(false);
        doThrow(new AppException(ErrorCode.UNAUTHENTICATED)).when(authService).validateAuthenticated();

        mockMvc.perform(get("/files/access-url").param("key", "documents/doc.pdf"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void getAccessUrl_PrivateFolder_Authenticated_Success() throws Exception {
        doNothing().when(authService).validateAuthenticated();

        AwsS3AccessUrlResponse response = AwsS3AccessUrlResponse.builder()
                .url("https://bucket.s3.amazonaws.com/documents/doc.pdf?token=123")
                .isPublic(false)
                .build();

        when(awsS3FileService.isPublicFolder("documents/doc.pdf")).thenReturn(false);
        when(awsS3FileService.getAccessUrl("documents/doc.pdf", 30L)).thenReturn(response);

        mockMvc.perform(get("/files/access-url")
                        .param("key", "documents/doc.pdf")
                        .param("ttl", "30"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.result.url").value("https://bucket.s3.amazonaws.com/documents/doc.pdf?token=123"))
                .andExpect(jsonPath("$.result.public").value(false));
    }

    @Test
    void getFileUrl_Success() throws Exception {
        when(awsS3FileService.getFileUrl("uploads/test.jpg"))
                .thenReturn("https://bucket.s3.amazonaws.com/uploads/test.jpg");

        mockMvc.perform(get("/files/url").param("key", "uploads/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("https://bucket.s3.amazonaws.com/uploads/test.jpg"));
    }

    @Test
    void downloadFile_Success() throws Exception {
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .contentLength(10L)
                .build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, "hello word".getBytes());

        when(awsS3FileService.getObject("uploads/test.jpg")).thenReturn(responseBytes);

        mockMvc.perform(get("/files/download").param("key", "uploads/test.jpg")).andExpect(status().isOk());
    }

    @Test
    void deleteFile_Success() throws Exception {
        doNothing().when(awsS3FileService).deleteObject("uploads/test.jpg");

        mockMvc.perform(delete("/files").param("key", "uploads/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("File deleted successfully: uploads/test.jpg"));
    }
}
