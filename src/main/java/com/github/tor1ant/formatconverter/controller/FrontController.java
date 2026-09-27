package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.config.UploadLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FrontController {

    private final UploadLimits uploadLimits;

    @GetMapping({"/", "/upload"})
    public String index(Model model) {
        // Лимиты нужны странице, чтобы отклонять слишком большие файлы ещё до отправки на сервер
        model.addAttribute("maxFileSize", uploadLimits.maxFileSize().toBytes());
        model.addAttribute("maxRequestSize", uploadLimits.maxRequestSize().toBytes());
        return "index";
    }
}
