package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.draft.DraftResponse;
import com.exam.system.dto.draft.DraftSaveRequest;
import com.exam.system.entity.AutoSaveDraft;
import com.exam.system.repository.AutoSaveDraftRepository;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.DraftService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drafts")
public class DraftController {

    private final DraftService draftService;
    private final AutoSaveDraftRepository draftRepository;

    public DraftController(DraftService draftService, AutoSaveDraftRepository draftRepository) {
        this.draftService = draftService;
        this.draftRepository = draftRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.get().getUserId();
        List<AutoSaveDraft> drafts = draftRepository.findByUserIdAndSavedAtAfter(userId, LocalDateTime.now().minusDays(30));
        List<Map<String, String>> items = drafts.stream().map(d -> Map.of(
                "formKey", d.getFormKey(),
                "updatedAt", d.getSavedAt().toString()
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(Map.of("items", items)));
    }

    @GetMapping("/{formKey}")
    public ResponseEntity<ApiResponse<DraftResponse>> get(@PathVariable String formKey) {
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(draftService.get(userId, formKey)));
    }

    @PutMapping("/{formKey}")
    public ResponseEntity<ApiResponse<DraftResponse>> save(@PathVariable String formKey,
                                                            @Valid @RequestBody DraftSaveRequest request) {
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(draftService.save(userId, formKey, request.getDraftJson())));
    }

    @DeleteMapping("/{formKey}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String formKey) {
        Long userId = UserContextHolder.get().getUserId();
        draftService.delete(userId, formKey);
        return ResponseEntity.ok(ApiResponse.successMessage("Draft deleted"));
    }
}
