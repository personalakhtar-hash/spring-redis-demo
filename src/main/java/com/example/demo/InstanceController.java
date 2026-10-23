package com.example.demo;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController
public class InstanceController {
 @Value("${server.port}") private int port;
 @GetMapping("/api/instance") public Map<String,Object> instance() { return Map.of("port",port,"application","spring-redis-demo"); }
}
