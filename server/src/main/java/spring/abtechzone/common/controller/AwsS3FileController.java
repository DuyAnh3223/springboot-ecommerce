package spring.abtechzone.common.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.common.dto.AwsS3FileResponse;
import spring.abtechzone.common.service.AwsS3FileService;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AwsS3FileController {

    AwsS3FileService awsS3FileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<AwsS3FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder) {
        return ApiResult.<AwsS3FileResponse>builder()
                .result(awsS3FileService.upload(file, folder))
                .build();
    }

    @GetMapping("/url")
    public ApiResult<String> getFileUrl(@RequestParam("key") String fileKey) {
        return ApiResult.<String>builder()
                .result(awsS3FileService.getFileUrl(fileKey))
                .build();
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("key") String fileKey) {
        ResponseBytes<GetObjectResponse> objectBytes = awsS3FileService.getObject(fileKey);
        GetObjectResponse response = objectBytes.response();

        String fileName = fileKey.contains("/") ? fileKey.substring(fileKey.lastIndexOf('/') + 1) : fileKey;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(response.contentType()))
                .contentLength(response.contentLength())
                .body(objectBytes.asByteArray());
    }

    @DeleteMapping
    public ApiResult<String> deleteFile(@RequestParam("key") String fileKey) {
        awsS3FileService.deleteObject(fileKey);
        return ApiResult.<String>builder()
                .result("File deleted successfully: " + fileKey)
                .build();
    }
}
