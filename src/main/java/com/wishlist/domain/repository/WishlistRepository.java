package com.wishlist.domain.repository;

import com.wishlist.domain.model.WishlistItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WishlistRepository extends MongoRepository<WishlistItem, Long> {

    List<WishlistItem> findWishlistItemByClientId(Integer clientId);
    List<WishlistItem> findWishlistItemByClientIdAndProductId(Integer clientId, Integer productId);
}