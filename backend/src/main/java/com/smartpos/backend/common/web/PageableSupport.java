package com.smartpos.backend.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageableSupport() {}

    public static Pageable resolve(Integer page, Integer size, Sort sort) {
        int p = page == null || page < 0 ? DEFAULT_PAGE : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return sort == null ? PageRequest.of(p, s) : PageRequest.of(p, s, sort);
    }

    public static Pageable resolve(Integer page, Integer size) {
        return resolve(page, size, null);
    }
}
