package http;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
public class HTTPServer {

    @RequestMapping("/")
    String home() {
        return "<h1>Hello World!</h1>";
    }

    public static void main(String[] args) {
        SpringApplication.run(HTTPServer.class, args);
    }

}