package com.mnp.commons.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified API response envelope.
 * <p>Success: {@code {"code":"MNPGCP200","data":...}}</p>
 * <p>Error:   {@code {"code":"MNPGCP500","message":"..."}}</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "MNPGCP200";
    public static final String ERROR_CODE = "MNPGCP500";

    private String code;
    private T data;
    private String message;

    /**
     * Construct a success response.
     *
     * @param data payload to return
     * @param <T>  payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> success(final T data) {
        return ApiResponse.<T>builder()
                .code(SUCCESS_CODE)
                .data(data)
                .build();
    }

    /**
     * Construct an error response.
     *
     * @param message human-readable error message
     * @return error envelope
     */
    public static <T> ApiResponse<T> error(final String message) {
        return ApiResponse.<T>builder()
                .code(ERROR_CODE)
                .message(message)
                .build();
    }
}
