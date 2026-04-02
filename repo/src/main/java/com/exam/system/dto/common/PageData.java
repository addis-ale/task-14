package com.exam.system.dto.common;

import java.util.ArrayList;
import java.util.List;

public class PageData<T> {

    private List<T> items = new ArrayList<>();
    private PaginationMeta pagination;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public PaginationMeta getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMeta pagination) {
        this.pagination = pagination;
    }
}
