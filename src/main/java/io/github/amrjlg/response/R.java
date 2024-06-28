package io.github.amrjlg.response;

import lombok.Data;

/**
 * @author lingjiang
 */
@Data
public class R<T> {
    private final String code;

    private final String message;

    private final T data;

    private final boolean success;

    public R(String code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    public static <S> R<S> success(S s) {
        return new R<>("200", "success", s, true);
    }

    public static <S> R<S> failed(String message) {
        return new R<>("400", message, null, false);
    }

    public static <S> R<S> failed(String code, String message) {
        return new R<>(code, message, null, false);
    }
}

