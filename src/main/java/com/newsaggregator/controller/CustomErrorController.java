package com.newsaggregator.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("statusCode", statusCode);
            
            if (statusCode == 404) {
                model.addAttribute("errorTitle", "Страница не найдена");
                model.addAttribute("errorMessage", "Запрашиваемая страница не существует.");
            } else if (statusCode == 500) {
                model.addAttribute("errorTitle", "Внутренняя ошибка сервера");
                model.addAttribute("errorMessage", "Произошла внутренняя ошибка сервера.");
                if (exception != null) {
                    model.addAttribute("exceptionDetails", exception.toString());
                }
                if (message != null) {
                    model.addAttribute("errorDetails", message.toString());
                }
            } else {
                model.addAttribute("errorTitle", "Ошибка " + statusCode);
                model.addAttribute("errorMessage", "Произошла ошибка при обработке запроса.");
            }
        }
        
        model.addAttribute("pageTitle", "Ошибка");
        return "error";
    }
}