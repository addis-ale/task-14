package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.term.TermRequest;
import com.exam.system.dto.term.TermResponse;
import com.exam.system.entity.AcademicTerm;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AcademicTermRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.service.AcademicTermService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicTermServiceImpl implements AcademicTermService {

    private final AcademicTermRepository termRepository;
    private final ExamSessionRepository examSessionRepository;
    private final PageDataBuilder pageDataBuilder;

    public AcademicTermServiceImpl(AcademicTermRepository termRepository,
                                   ExamSessionRepository examSessionRepository,
                                   PageDataBuilder pageDataBuilder) {
        this.termRepository = termRepository;
        this.examSessionRepository = examSessionRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<TermResponse> listTerms(int page, int size) {
        Page<AcademicTerm> result = termRepository.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return pageDataBuilder.from(result, this::toResponse, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public TermResponse getTerm(Long termId) {
        AcademicTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Term not found"));
        return toResponse(term);
    }

    @Override
    @Transactional
    public TermResponse createTerm(TermRequest request) {
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateCurrentActive();
        }
        AcademicTerm term = new AcademicTerm();
        applyRequest(term, request);
        return toResponse(termRepository.save(term));
    }

    @Override
    @Transactional
    public TermResponse updateTerm(Long termId, TermRequest request) {
        AcademicTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Term not found"));
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateCurrentActive();
        }
        applyRequest(term, request);
        return toResponse(termRepository.save(term));
    }

    @Override
    @Transactional
    public void deleteTerm(Long termId) {
        AcademicTerm term = termRepository.findById(termId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Term not found"));
        long sessionCount = examSessionRepository.countByTermId(termId);
        if (sessionCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Cannot delete term referenced by exam sessions");
        }
        termRepository.delete(term);
    }

    private void applyRequest(AcademicTerm term, TermRequest request) {
        term.setName(InputSanitizer.sanitize(request.getName()));
        term.setStartDate(request.getStartDate());
        term.setEndDate(request.getEndDate());
        term.setActive(Boolean.TRUE.equals(request.getIsActive()));
    }

    private void deactivateCurrentActive() {
        termRepository.findByActiveTrue().ifPresent(active -> {
            active.setActive(false);
            termRepository.save(active);
        });
    }

    private TermResponse toResponse(AcademicTerm term) {
        TermResponse response = new TermResponse();
        response.setId(term.getId());
        response.setName(term.getName());
        response.setStartDate(term.getStartDate());
        response.setEndDate(term.getEndDate());
        response.setActive(term.isActive());
        return response;
    }
}
