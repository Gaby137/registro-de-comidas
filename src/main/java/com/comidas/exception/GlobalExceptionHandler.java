package com.comidas.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Manejador global de excepciones para la capa MVC (vistas Thymeleaf).
 * Renderiza templates/error.html (la misma vista que usa Spring para 404/403/500).
 * Incluye protección anti-Open Redirect al redireccionar por Referer.
 */
@Slf4j
@ControllerAdvice(basePackages = "com.comidas.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return vistaError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Reglas de negocio: vuelve a la página anterior mostrando el mensaje (fragmento alertas). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException exc, HttpServletRequest request) {
        log.warn("Validación fallida: {}", exc.getMessage());
        return redirigirConError(request, exc.getMessage());
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ModelAndView handleMaxSizeException(Exception exc, HttpServletRequest request) {
        log.warn("Error de subida de archivo: {}", exc.getMessage());
        return redirigirConError(request, "No se pudo subir el archivo: revisá que no supere el tamaño máximo permitido.");
    }

    /** Ej: @PreAuthorize que falla dentro del controller. Se relanza para que Spring Security responda 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    /** Parámetros de URL inválidos (ej: ?rol=INEXISTENTE o ?sort=campoInexistente): 400, no 500. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            PropertyReferenceException.class})
    public ModelAndView handleParametroInvalido(Exception ex) {
        log.warn("Parámetro inválido: {}", ex.getMessage());
        return vistaError(HttpStatus.BAD_REQUEST, "La solicitud tiene parámetros inválidos.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        // Excepciones propias de Spring MVC (400, 405, 415, ...) conservan su código HTTP;
        // sin mensaje propio, error.html muestra el texto genérico de ese código
        if (ex instanceof ErrorResponse errorResponse) {
            log.warn("Error de la solicitud ({}): {}", errorResponse.getStatusCode(), ex.getMessage());
            return vistaError(HttpStatus.valueOf(errorResponse.getStatusCode().value()), null);
        }
        log.error("Error inesperado: ", ex);
        return vistaError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Por favor, intentá más tarde.");
    }

    private ModelAndView vistaError(HttpStatus status, String mensaje) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", status.value());
        if (mensaje != null) {
            mav.addObject("errorMessage", mensaje);
        }
        mav.setStatus(status);
        return mav;
    }

    private ModelAndView redirigirConError(HttpServletRequest request, String mensaje) {
        FlashMap flashMap = new FlashMap();
        flashMap.put("mensajeError", mensaje);
        FlashMapManager flashMapManager = RequestContextUtils.getFlashMapManager(request);
        if (flashMapManager != null) {
            flashMapManager.saveOutputFlashMap(flashMap, request, null);
        }
        return new ModelAndView("redirect:" + obtenerRedirectSeguro(request, "/"));
    }

    /**
     * Devuelve el path del Referer solo si es del mismo host, para prevenir Open Redirect.
     * Rechaza paths tipo "//otro-dominio.com" que el navegador interpretaría como externos.
     */
    private String obtenerRedirectSeguro(HttpServletRequest request, String fallbackUrl) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return fallbackUrl;
        }

        try {
            URI refererUri = URI.create(referer);
            String path = refererUri.getRawPath();

            if (request.getServerName().equalsIgnoreCase(refererUri.getHost())
                    && path != null && path.startsWith("/") && !path.startsWith("//")) {
                String query = refererUri.getRawQuery();
                return path + (query != null ? "?" + query : "");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Referer con formato inválido: {}", referer);
        }

        return fallbackUrl;
    }
}
