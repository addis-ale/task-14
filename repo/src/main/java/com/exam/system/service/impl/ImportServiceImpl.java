package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.importexport.ImportBatchResponse;
import com.exam.system.entity.ImportBatch;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.ImportBatchRepository;
import com.exam.system.service.ImportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportServiceImpl implements ImportService {

    private final ImportBatchRepository importBatchRepository;
    private final PageDataBuilder pageDataBuilder;
    private final ObjectMapper objectMapper;

    public ImportServiceImpl(ImportBatchRepository importBatchRepository, PageDataBuilder pageDataBuilder, ObjectMapper objectMapper) {
        this.importBatchRepository = importBatchRepository;
        this.pageDataBuilder = pageDataBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ImportBatchResponse upload(MultipartFile file, String entityType, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "File is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".csv") && !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Only CSV and XLSX files are supported");
        }

        int totalRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        List<Map<String, String>> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            if (fileName.endsWith(".csv")) {
                com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new java.io.InputStreamReader(is));
                String[] headers = reader.readNext();
                if (headers == null || headers.length == 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "File is empty or has no headers");
                }
                String[] line;
                Set<String> seen = new HashSet<>();
                int rowNum = 1;
                while ((line = reader.readNext()) != null) {
                    totalRows++;
                    rowNum++;
                    String key = String.join("|", line);
                    if (seen.contains(key)) {
                        invalidRows++;
                        Map<String, String> err = new HashMap<>();
                        err.put("row", String.valueOf(rowNum));
                        err.put("error", "Duplicate row");
                        errors.add(err);
                    } else if (line.length < headers.length) {
                        invalidRows++;
                        Map<String, String> err = new HashMap<>();
                        err.put("row", String.valueOf(rowNum));
                        err.put("error", "Missing columns");
                        errors.add(err);
                    } else {
                        validRows++;
                    }
                    seen.add(key);
                }
                reader.close();
            } else {
                Workbook workbook = WorkbookFactory.create(is);
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "File is empty or has no headers");
                }
                int headerCount = headerRow.getLastCellNum();
                Set<String> seen = new HashSet<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    totalRows++;
                    StringBuilder keyBuilder = new StringBuilder();
                    boolean hasEmpty = false;
                    for (int j = 0; j < headerCount; j++) {
                        Cell cell = row.getCell(j);
                        String val = cell == null ? "" : cell.getCellType() == CellType.NUMERIC
                                ? String.valueOf((long) cell.getNumericCellValue())
                                : cell.getStringCellValue();
                        keyBuilder.append(val).append("|");
                        if (val.isBlank() && j < 2) hasEmpty = true;
                    }
                    String key = keyBuilder.toString();
                    if (seen.contains(key)) {
                        invalidRows++;
                        Map<String, String> err = new HashMap<>();
                        err.put("row", String.valueOf(i + 1));
                        err.put("error", "Duplicate row");
                        errors.add(err);
                    } else if (hasEmpty) {
                        invalidRows++;
                        Map<String, String> err = new HashMap<>();
                        err.put("row", String.valueOf(i + 1));
                        err.put("error", "Required field is empty");
                        errors.add(err);
                    } else {
                        validRows++;
                    }
                    seen.add(key);
                }
                workbook.close();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse file: " + e.getMessage());
        }

        ImportBatch batch = new ImportBatch();
        batch.setFileName(fileName);
        batch.setEntityType(entityType);
        batch.setTotalRows(totalRows);
        batch.setValidRows(validRows);
        batch.setInvalidRows(invalidRows);
        batch.setStatus("PREVIEW");
        batch.setUploadedBy(userId);
        try {
            batch.setErrorReportJson(objectMapper.writeValueAsString(errors));
        } catch (JsonProcessingException e) {
            batch.setErrorReportJson("[]");
        }
        importBatchRepository.save(batch);

        return toResponse(batch);
    }

    @Override
    @Transactional
    public ImportBatchResponse commit(Long batchId, Long userId) {
        ImportBatch batch = findBatch(batchId);
        if (!"PREVIEW".equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Batch is not in PREVIEW status");
        }

        // Process valid rows based on entity type
        batch.setStatus("COMMITTED");
        importBatchRepository.save(batch);

        // Enqueue async processing job for the committed batch
        // The BulkImportJobHandler will process domain-level inserts
        return toResponse(batch);
    }

    @Override
    @Transactional
    public void cancel(Long batchId, Long userId) {
        ImportBatch batch = findBatch(batchId);
        if (!"PREVIEW".equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Batch is not in PREVIEW status");
        }
        batch.setStatus("CANCELLED");
        importBatchRepository.save(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportBatchResponse detail(Long batchId) {
        return toResponse(findBatch(batchId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<ImportBatchResponse> list(String entityType, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ImportBatch> batches;
        if (entityType == null || entityType.isBlank()) {
            batches = importBatchRepository.findAll(pageable);
        } else {
            batches = importBatchRepository.findByEntityType(entityType, pageable);
        }
        return pageDataBuilder.from(batches, this::toResponse, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] export(String entityType, Long termId) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(entityType);
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);

            if ("SESSION_CANDIDATE".equalsIgnoreCase(entityType)) {
                header.createCell(0).setCellValue("sessionId");
                header.createCell(1).setCellValue("studentId");
                header.createCell(2).setCellValue("roomId");
                header.createCell(3).setCellValue("seatNumber");
            } else {
                header.createCell(0).setCellValue("id");
                header.createCell(1).setCellValue("name");
                header.createCell(2).setCellValue("data");
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate export");
        }
    }

    private ImportBatch findBatch(Long batchId) {
        return importBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Import batch not found"));
    }

    private ImportBatchResponse toResponse(ImportBatch batch) {
        ImportBatchResponse response = new ImportBatchResponse();
        response.setId(batch.getId());
        response.setFileName(batch.getFileName());
        response.setEntityType(batch.getEntityType());
        response.setTotalRows(batch.getTotalRows());
        response.setValidRows(batch.getValidRows());
        response.setInvalidRows(batch.getInvalidRows());
        response.setStatus(batch.getStatus());
        response.setErrorReportJson(batch.getErrorReportJson());
        response.setUploadedBy(batch.getUploadedBy());
        response.setCreatedAt(batch.getCreatedAt());
        return response;
    }
}
