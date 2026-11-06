package com.ace3i.connect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("Une erreur non gérée est survenue : {}", ex.getMessage(), ex);
        model.addAttribute("error", "Une erreur interne est survenue. Veuillez contacter votre administrateur.");
        model.addAttribute("exceptionMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        log.warn("Page introuvable : {}", ex.getMessage());
        model.addAttribute("error", "La page demandée n'existe pas.");
        return "error";
    }
}
