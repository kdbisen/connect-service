package com.adyanta.connect.service;

import com.adyanta.connect.domain.document.ProcessingAuditLog;
import com.adyanta.connect.domain.document.ProcessingRequest;
import com.adyanta.connect.domain.enums.ProcessingStatus;
import com.adyanta.connect.repository.ProcessingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for audit logging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final ProcessingAuditLogRepository auditLogRepository;
    
    /**
     * Log the start of a processing step
     */
    public Mono<ProcessingAuditLog> logStepStart(String processingRequestId, String stepName) {
        ProcessingAuditLog auditLog = ProcessingAuditLog.builder()
                .processingRequestId(processingRequestId)
                .stepName(stepName)
                .status(ProcessingStatus.PROCESSING)
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        return auditLogRepository.save(auditLog)
                .doOnSuccess(log -> log.debug("Step start logged: {} for request: {}", stepName, processingRequestId));
    }
    
    /**
     * Log successful completion of a processing step
     */
    public Mono<ProcessingAuditLog> logStepSuccess(String processingRequestId, String stepName, ProcessingRequest request) {
        return auditLogRepository.findByProcessingRequestIdAndStepName(processingRequestId, stepName)
                .next()
                .map(existingLog -> existingLog.toBuilder()
                        .status(ProcessingStatus.COMPLETED)
                        .endTime(LocalDateTime.now())
                        .durationMs(calculateDuration(existingLog.getStartTime(), LocalDateTime.now()))
                        .outputData(extractOutputData(request, stepName))
                        .build())
                .flatMap(auditLogRepository::save)
                .doOnSuccess(log -> log.debug("Step success logged: {} for request: {}", stepName, processingRequestId));
    }
    
    /**
     * Log failure of a processing step
     */
    public Mono<ProcessingAuditLog> logStepFailure(String processingRequestId, String stepName, String errorMessage) {
        return auditLogRepository.findByProcessingRequestIdAndStepName(processingRequestId, stepName)
                .next()
                .map(existingLog -> existingLog.toBuilder()
                        .status(ProcessingStatus.FAILED)
                        .endTime(LocalDateTime.now())
                        .durationMs(calculateDuration(existingLog.getStartTime(), LocalDateTime.now()))
                        .errorMessage(errorMessage)
                        .build())
                .flatMap(auditLogRepository::save)
                .doOnSuccess(log -> log.debug("Step failure logged: {} for request: {}", stepName, processingRequestId));
    }
    
    private Long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    private String extractOutputData(ProcessingRequest request, String stepName) {
        switch (stepName) {
            case "XML_TO_JSON_CONVERSION":
                return request.getConvertedJsonPayload();
            case "EXTERNAL_API_CALL":
                return request.getExternalApiResponse();
            case "FENERGO_API_CALL":
                return request.getFenergoResponse();
            default:
                return "Step completed";
        }
    }
}
