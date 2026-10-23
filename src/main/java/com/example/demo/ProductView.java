package com.example.demo;
import java.io.Serializable;
import java.math.BigDecimal;
public record ProductView(Long id, String name, BigDecimal price) implements Serializable {
 static ProductView from(Product p) { return new ProductView(p.getId(),p.getName(),p.getPrice()); }
}
