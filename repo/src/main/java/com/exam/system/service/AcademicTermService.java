package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.term.TermRequest;
import com.exam.system.dto.term.TermResponse;

public interface AcademicTermService {

    PageData<TermResponse> listTerms(int page, int size);

    TermResponse getTerm(Long termId);

    TermResponse createTerm(TermRequest request);

    TermResponse updateTerm(Long termId, TermRequest request);

    void deleteTerm(Long termId);
}
