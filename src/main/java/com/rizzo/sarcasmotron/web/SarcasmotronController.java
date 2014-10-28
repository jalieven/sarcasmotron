package com.rizzo.sarcasmotron.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SarcasmotronController {

    @RequestMapping(value = "/hello")
    public String hello() {
        return "hello";
    }

}
