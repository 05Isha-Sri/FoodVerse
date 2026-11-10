package com.foodverse.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "items")          
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User customer;

    @OneToMany(mappedBy = "cart",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();
}