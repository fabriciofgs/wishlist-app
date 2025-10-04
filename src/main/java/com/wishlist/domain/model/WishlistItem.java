package com.wishlist.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    @Transient
    public static final String SEQUENCE_NAME = "wishlist_sequence";

    @Id
    private Long id;
    @NotNull
    private Integer clientId;
    private String clientName;
    @NotNull
    private Integer productId;
    private String productName;
    private LocalDate date = LocalDate.now();
}
