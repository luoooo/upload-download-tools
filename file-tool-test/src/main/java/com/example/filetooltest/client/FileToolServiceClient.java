package com.example.filetooltest.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FileToolServiceClient {

    @Value("${file.tool.service.url}")
    private String serviceUrl;

    private final RestTemplate restTemplate;

    public FileToolServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> uploadFile(MultipartFile file, String taskName, String callbackUrl, String callbackParams) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("taskName", taskName);
            if (callbackUrl != null) {
                body.add("callbackUrl", callbackUrl);
            }
            if (callbackParams != null) {
                body.add("callbackParams", callbackParams);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                serviceUrl + "/tasks",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error uploading file to service", e);
            throw new RuntimeException("Failed to upload file to service", e);
        }
    }

    public Map<String, Object> getTaskStatus(Long taskId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                serviceUrl + "/tasks/" + taskId,
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting task status from service", e);
            throw new RuntimeException("Failed to get task status from service", e);
        }
    }

    public Map<String, Object> downloadFile(Long taskId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                serviceUrl + "/tasks/" + taskId + "/download",
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error downloading file from service", e);
            throw new RuntimeException("Failed to download file from service", e);
        }
    }

    public Map<String, Object> createExportTask(String taskName, String callbackUrl, String callbackParams) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("taskName", taskName);
            if (callbackUrl != null) {
                body.put("callbackUrl", callbackUrl);
            }
            if (callbackParams != null) {
                body.put("callbackParams", callbackParams);
            }

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                serviceUrl + "/export",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating export task", e);
            throw new RuntimeException("Failed to create export task", e);
        }
    }
} 