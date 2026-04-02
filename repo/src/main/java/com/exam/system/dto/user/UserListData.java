package com.exam.system.dto.user;

import java.util.ArrayList;
import java.util.List;

public class UserListData {

    private List<UserResponse> items = new ArrayList<>();
    private PaginationDto pagination;

    public List<UserResponse> getItems() {
        return items;
    }

    public void setItems(List<UserResponse> items) {
        this.items = items;
    }

    public PaginationDto getPagination() {
        return pagination;
    }

    public void setPagination(PaginationDto pagination) {
        this.pagination = pagination;
    }
}
