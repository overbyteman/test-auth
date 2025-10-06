package com.seccreto.service.auth.api.exception;

import com.seccreto.service.auth.api.dto.ErrorResponse;
import com.seccreto.service.auth.service.exception.AuthenticationException;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void shouldHandleResourceNotFound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users/123");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Usuário não encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Usuário não encontrado");
        assertThat(response.getBody().getPath()).isEqualTo("/api/users/123");
    }

    @Test
    void shouldHandleAuthentication() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(
                new AuthenticationException("Credenciais inválidas"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("Credenciais inválidas");
    }

    @Test
    void shouldHandleConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new ConflictException("Email já existe"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email já existe");
    }

    @Test
    void shouldHandleValidation() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users");

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new ValidationException("Dados inválidos"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Dados inválidos");
    }

    @Test
    void shouldHandleIllegalArgument() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Argumento inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Argumento inválido");
    }

    @Test
    void shouldHandleGenericException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/any");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Erro interno do servidor");
        assertThat(response.getBody().getDetails()).contains("Contate o suporte técnico");
    }

    @Test
    void shouldHandleMethodArgumentNotValid() throws NoSuchMethodException {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(servletRequest);

        Method method = SampleValidationTarget.class.getDeclaredMethod("sample", String.class);
        var parameter = new org.springframework.core.MethodParameter(method, 0);

        var bindingResult = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "user");
        bindingResult.addError(new org.springframework.validation.FieldError("user", "name", "Nome é obrigatório"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getDetails()).contains("name: Nome é obrigatório");
    }

    private static class SampleValidationTarget {
        @SuppressWarnings("unused")
        void sample(String value) {
        }
    }
}
