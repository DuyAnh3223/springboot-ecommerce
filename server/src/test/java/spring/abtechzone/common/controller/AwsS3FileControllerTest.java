package spring.abtechzone.common.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.service.AwsS3FileService;

@ExtendWith(MockitoExtension.class)
class AwsS3FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AwsS3FileService awsS3FileService;

    @InjectMocks
    private AwsS3FileController awsS3FileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(awsS3FileController).build();
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
