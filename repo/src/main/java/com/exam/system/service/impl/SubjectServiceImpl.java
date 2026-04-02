package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.subject.SubjectRequest;
import com.exam.system.dto.subject.SubjectResponse;
import com.exam.system.entity.Subject;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.SubjectRepository;
import com.exam.system.service.SubjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ExamSessionRepository examSessionRepository;
    private final PageDataBuilder pageDataBuilder;

    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              ExamSessionRepository examSessionRepository,
                              PageDataBuilder pageDataBuilder) {
        this.subjectRepository = subjectRepository;
        this.examSessionRepository = examSessionRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<SubjectResponse> listSubjects(Long gradeId, int page, int size) {
        Page<Subject> result;
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size));
        if (gradeId == null) {
            result = subjectRepository.findAll(pageable);
        } else {
            result = subjectRepository.findByGradeId(gradeId, pageable);
        }
        return pageDataBuilder.from(result, this::toResponse, page, size);
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = new Subject();
        subject.setName(InputSanitizer.sanitize(request.getName()));
        subject.setGradeId(request.getGradeId());
        return toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Subject not found"));
        subject.setName(InputSanitizer.sanitize(request.getName()));
        subject.setGradeId(request.getGradeId());
        return toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void deleteSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Subject not found"));
        long referenced = examSessionRepository.count((root, query, cb) -> cb.equal(root.get("subjectId"), subjectId));
        if (referenced > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Cannot delete subject referenced by exam sessions");
        }
        subjectRepository.delete(subject);
    }

    private SubjectResponse toResponse(Subject subject) {
        SubjectResponse response = new SubjectResponse();
        response.setId(subject.getId());
        response.setName(subject.getName());
        response.setGradeId(subject.getGradeId());
        return response;
    }
}
