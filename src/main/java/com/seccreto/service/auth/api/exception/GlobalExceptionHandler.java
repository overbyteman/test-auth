package com.seccreto.service.auth.api.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seccreto.service.auth.api.dto.ErrorResponse;
import com.seccreto.service.auth.service.exception.AuthenticationException;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.domain.security.InputValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler global para traduzir exceções em respostas HTTP consistentes.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InputValidationException.class)
    public ResponseEntity<ErrorResponse> handleInputValidation(InputValidationException ex, HttpServletRequest request) {
        List<String> details = List.of(
                "Validation Type: " + ex.getValidationType(),
                "Input: " + (ex.getInput().length() > 50 ? ex.getInput().substring(0, 50) + "..." : ex.getInput())
        );
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
        return buildError(HttpStatus.BAD_REQUEST, "Violação de restrições", request.getRequestURI(), details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    // ===== EXCEÇÕES DE BANCO DE DADOS =====
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de acesso aos dados", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Violação de integridade dos dados", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Conflito de concorrência - dados foram modificados por outro usuário", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQL(SQLException ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de banco de dados", request.getRequestURI(), 
                List.of("SQL State: " + ex.getSQLState(), "Error Code: " + ex.getErrorCode()));
    }

    // ===== EXCEÇÕES DE JSON =====
    
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessing(JsonProcessingException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Erro ao processar JSON", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }

    // ===== EXCEÇÕES DE REDE =====
    
    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity<ErrorResponse> handleUnknownHost(UnknownHostException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Endereço de rede inválido", request.getRequestURI(), 
                List.of("Host: " + ex.getMessage()));
    }

    // ===== EXCEÇÕES DE REQUISIÇÃO HTTP =====
    
    // Override the parent class method to avoid ambiguity
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_ALLOWED")
                .message("Método HTTP não suportado")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(List.of(
                    "Método: " + ex.getMethod(),
                    "Métodos suportados: " + String.join(", ", ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[0])
                ))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Erro ao ler mensagem HTTP")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(List.of("Detalhes: " + ex.getMessage()))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message("Parâmetro obrigatório ausente")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(List.of("Parâmetro: " + ex.getParameterName(), "Tipo: " + ex.getParameterType()))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Tipo de parâmetro inválido", request.getRequestURI(), 
                List.of("Parâmetro: " + ex.getName(), "Valor: " + ex.getValue(), "Tipo esperado: " + ex.getRequiredType().getSimpleName()));
    }

    // ===== EXCEÇÕES DE SEGURANÇA =====
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Erro de segurança", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(IllegalAccessException.class)
    public ResponseEntity<ErrorResponse> handleIllegalAccess(IllegalAccessException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Acesso negado", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }

    // ===== EXCEÇÕES DE NEGÓCIO =====
    
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_IMPLEMENTED, "Operação não suportada", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Estado inválido para a operação", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }

    // ===== EXCEÇÕES DE TIMEOUT =====
    
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(java.util.concurrent.TimeoutException ex, HttpServletRequest request) {
        return buildError(HttpStatus.REQUEST_TIMEOUT, "Timeout na operação", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }

    // ===== EXCEÇÕES CUSTOMIZADAS DO SISTEMA =====
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Erro de regra de negócio", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionExpired(SessionExpiredException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Sessão expirada", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Token inválido", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(InsufficientPermissionsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermissions(InsufficientPermissionsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Permissões insuficientes", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }
    
    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Tenant não encontrado", request.getRequestURI(), 
                List.of("Detalhes: " + ex.getMessage()));
    }

    // ===== EXCEÇÃO GENÉRICA =====
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request.getRequestURI(), 
                List.of("Tipo: " + ex.getClass().getSimpleName(), "Detalhes: " + ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());
        ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Erro de validação", request.getDescription(false).replace("uri=", ""), details);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, String path, List<String> details) {
        ErrorResponse error = new ErrorResponse(status.value(), status.getReasonPhrase(), message, path, details);
        return ResponseEntity.status(status).body(error);
    }
}
