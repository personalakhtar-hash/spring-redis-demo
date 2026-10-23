package com.example.demo;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/products")
public class ProductController {
 private final ProductService service;
 public ProductController(ProductService service) { this.service=service; }
 @GetMapping public List<ProductView> list() { return service.list(); }
 @GetMapping("/{id}") public ProductView get(@PathVariable long id) { return service.get(id); }
 @PostMapping public ResponseEntity<ProductView> create(@Valid @RequestBody ProductRequest request) {
  ProductView result=service.create(request);
  return ResponseEntity.created(URI.create("/api/products/"+result.id())).body(result);
 }
 @PutMapping("/{id}") public ProductView update(@PathVariable long id,@Valid @RequestBody ProductRequest request) { return service.update(id,request); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
