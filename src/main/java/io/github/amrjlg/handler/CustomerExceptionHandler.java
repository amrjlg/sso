package io.github.amrjlg.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.amrjlg.response.R;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author lingjiang
 */
@ControllerAdvice
public class CustomerExceptionHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @ExceptionHandler(RuntimeException.class)
    public void handler(RuntimeException e, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = e.getMessage();
        R<Void> r = R.failed(message);
        response.getOutputStream().write(objectMapper.writeValueAsString(r).getBytes(StandardCharsets.UTF_8));
    }
}
