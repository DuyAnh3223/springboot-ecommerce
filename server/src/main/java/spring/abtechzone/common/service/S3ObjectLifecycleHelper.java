package spring.abtechzone.common.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class S3ObjectLifecycleHelper {

    AwsS3FileService awsS3FileService;

    /**
     * Schedules deletion of an S3 object key or URL after the active transaction successfully commits.
     * If no transaction is active, attempts immediate deletion.
     */
    public void deleteAfterCommit(String s3KeyOrUrl) {
        String s3Key = awsS3FileService.extractS3Key(s3KeyOrUrl);
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        awsS3FileService.deleteObject(s3Key);
                    } catch (Exception e) {
                        log.error("Failed to delete S3 object after commit for key {}: {}", s3Key, e.getMessage(), e);
                    }
                }
            });
        } else {
            try {
                awsS3FileService.deleteObject(s3Key);
            } catch (Exception e) {
                log.error("Failed to delete S3 object for key {}: {}", s3Key, e.getMessage(), e);
            }
        }
    }
}
