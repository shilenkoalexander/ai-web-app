package org.donntu.ai.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/test")
    public String test(){
        return "Опа здарова тебе с сервачка, братан";
    }
}
