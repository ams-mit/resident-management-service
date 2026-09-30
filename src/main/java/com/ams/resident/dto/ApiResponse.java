package com.ams.resident.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private Object meta;

    public ApiResponse() {
        this.meta = Collections.emptyMap();
    }

    public ApiResponse(T data) {
        this.data = data;
        this.meta = Collections.emptyMap();
    }

    public ApiResponse(T data, Object meta) {
        this.data = data;
        this.meta = meta != null ? meta : Collections.emptyMap();
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> of(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getMeta() {
        return meta;
    }

    public void setMeta(Object meta) {
        this.meta = meta != null ? meta : Collections.emptyMap();
    }
}
