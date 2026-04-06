package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.service.impl.PageDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PageDataBuilder — converts Spring Data Page to custom PageData DTO.
 */
@DisplayName("PageDataBuilder Tests")
class PageDataBuilderTest {

    private PageDataBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new PageDataBuilder();
    }

    @Test
    @DisplayName("Should convert Page to PageData with correct items")
    void testConvertWithItems() {
        List<String> content = List.of("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);

        PageData<String> result = builder.from(page, s -> s, 1, 10);

        assertEquals(3, result.getItems().size());
        assertEquals("item1", result.getItems().get(0));
    }

    @Test
    @DisplayName("Should set pagination metadata correctly")
    void testPaginationMeta() {
        List<String> content = List.of("a", "b");
        Page<String> page = new PageImpl<>(content, PageRequest.of(1, 5), 12);

        PageData<String> result = builder.from(page, s -> s, 2, 5);

        assertEquals(2, result.getPagination().getPage());
        assertEquals(5, result.getPagination().getSize());
        assertEquals(12, result.getPagination().getTotalItems());
        assertEquals(3, result.getPagination().getTotalPages());
    }

    @Test
    @DisplayName("Should handle empty page")
    void testEmptyPage() {
        Page<String> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        PageData<String> result = builder.from(page, s -> s, 1, 10);

        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getPagination().getTotalItems());
        assertEquals(0, result.getPagination().getTotalPages());
    }

    @Test
    @DisplayName("Should apply mapper function to transform items")
    void testMapperFunction() {
        List<Integer> content = List.of(1, 2, 3);
        Page<Integer> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);

        PageData<String> result = builder.from(page, i -> "Item-" + i, 1, 10);

        assertEquals("Item-1", result.getItems().get(0));
        assertEquals("Item-2", result.getItems().get(1));
        assertEquals("Item-3", result.getItems().get(2));
    }
}
