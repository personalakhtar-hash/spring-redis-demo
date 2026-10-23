package com.example.demo;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name="products")
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, length=200) private String name;
 @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
 protected Product() {}
 public Product(String name, BigDecimal price) { this.name=name; this.price=price; }
 public Long getId() { return id; }
 public String getName() { return name; }
 public BigDecimal getPrice() { return price; }
 public void update(String name, BigDecimal price) { this.name=name; this.price=price; }
}
