package com.example.demo;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record ProductRequest(@NotBlank @Size(max=200) String name,
 @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price) {}
