package com.exam.system.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice(annotations = Controller.class)
public class SanitizationRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter,
                            java.lang.reflect.Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                java.lang.reflect.Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        sanitizeRecursive(body, new IdentityHashMap<>());
        return body;
    }

    private void sanitizeRecursive(Object value, Map<Object, Boolean> visited) {
        if (value == null || visited.containsKey(value)) {
            return;
        }
        visited.put(value, Boolean.TRUE);

        Package typePackage = value.getClass().getPackage();
        if (typePackage != null && typePackage.getName().startsWith("java.")) {
            return;
        }

        if (value instanceof String) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                sanitizeRecursive(item, visited);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                sanitizeRecursive(item, visited);
            }
            return;
        }

        Field[] fields = value.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                if (fieldValue instanceof String text) {
                    field.set(value, InputSanitizer.sanitize(text));
                } else {
                    sanitizeRecursive(fieldValue, visited);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
