package spring.abtechzone.common.dto;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsS3FileResponse {
    String fileKey;
    String fileUrl;
    String contentType;
    Long size;
    boolean isPublic;
    Instant expiresAt;
}
