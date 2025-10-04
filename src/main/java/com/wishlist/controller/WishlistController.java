package com.wishlist.controller;

import com.wishlist.domain.model.Wishlist;
import com.wishlist.domain.model.WishlistItem;
import com.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/{id}")
    public ResponseEntity<WishlistItem> getWishlistItemById(@PathVariable final Long id) {
        WishlistItem item = wishlistService.getWishlistItemById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Wishlist> getWishlistbyClientId(@PathVariable("clientId") final Integer clientId) {
        Wishlist wishlist = wishlistService.getWishlistByClientId(clientId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/client/{clientId}/product/{productId}")
    public ResponseEntity<WishlistItem> getWishlistByClientIdAndProductId(@PathVariable("clientId") final Integer clientId,
                                                                          @PathVariable("productId") final Integer productId) {
        WishlistItem item = wishlistService.getWishlistByClientIdAndProductId(clientId, productId);
        return ResponseEntity.ok(item);
    }

    @PostMapping("/add_item")
    public ResponseEntity<WishlistItem> addWishlistItem(@Valid @RequestBody final WishlistItem wishlistItem) {
        WishlistItem saved = wishlistService.addWishlistItem(wishlistItem);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/add_list")
    public ResponseEntity<String> addWishlist(@Valid @RequestBody final Wishlist wishlist) {
        wishlistService.addWishlist(wishlist);
        return ResponseEntity.ok("Itens adicionados a wishlist com sucesso");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteWishlistItemById(@PathVariable("id") final Long id) {
        wishlistService.deleteWishlistItemById(id);
        return ResponseEntity.ok("Item removido com sucesso");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteWishlist() {
        wishlistService.deleteWishlist();
        return ResponseEntity.ok("Todos os items foram removidos da wishlist com sucesso");
    }
}