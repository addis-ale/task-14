package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.importexport.ImportBatchResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImportService {

    ImportBatchResponse upload(MultipartFile file, String entityType, Long userId);

    ImportBatchResponse commit(Long batchId, Long userId);

    void cancel(Long batchId, Long userId);

    ImportBatchResponse detail(Long batchId);

    PageData<ImportBatchResponse> list(String entityType, int page, int size);

    byte[] export(String entityType, Long termId);
}
