package com.example.demo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class ProductService {
 private static final Logger log=LoggerFactory.getLogger(ProductService.class);
 private final ProductRepository repository;
 public ProductService(ProductRepository repository) { this.repository=repository; }
 private Product find(long id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found")); }
 @Cacheable(value="products",key="#id")
 @Transactional(readOnly=true)
 public ProductView get(long id) { log.info("Cache miss: loading product {} from PostgreSQL",id); return ProductView.from(find(id)); }
 @Transactional(readOnly=true)
 public List<ProductView> list() { return repository.findAll().stream().map(ProductView::from).toList(); }
 @Transactional
 public ProductView create(ProductRequest request) { return ProductView.from(repository.save(new Product(request.name(),request.price()))); }
 @Transactional
 @CacheEvict(value="products",key="#id")
 public ProductView update(long id,ProductRequest request) { Product product=find(id); product.update(request.name(),request.price()); return ProductView.from(product); }
 @Transactional
 @CacheEvict(value="products",key="#id")
 public void delete(long id) { repository.delete(find(id)); }
}
