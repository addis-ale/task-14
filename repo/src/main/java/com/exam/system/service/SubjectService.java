package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.subject.SubjectRequest;
import com.exam.system.dto.subject.SubjectResponse;

public interface SubjectService {

    PageData<SubjectResponse> listSubjects(Long gradeId, int page, int size);

    SubjectResponse createSubject(SubjectRequest request);

    SubjectResponse updateSubject(Long subjectId, SubjectRequest request);

    void deleteSubject(Long subjectId);
}
