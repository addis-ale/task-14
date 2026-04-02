package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.common.PaginationMeta;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageDataBuilder {

    public <S, T> PageData<T> from(Page<S> page, Function<S, T> mapper, int requestedPage, int requestedSize) {
        PageData<T> response = new PageData<>();
        List<T> items = page.getContent().stream().map(mapper).toList();
        response.setItems(items);

        PaginationMeta pagination = new PaginationMeta();
        pagination.setPage(requestedPage);
        pagination.setSize(requestedSize);
        pagination.setTotalItems(page.getTotalElements());
        pagination.setTotalPages(page.getTotalPages());
        response.setPagination(pagination);
        return response;
    }
}
