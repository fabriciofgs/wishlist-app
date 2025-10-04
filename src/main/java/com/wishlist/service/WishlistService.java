package com.wishlist.service;

import com.wishlist.domain.model.Wishlist;
import com.wishlist.domain.model.WishlistItem;
import com.wishlist.domain.repository.WishlistRepository;
import com.wishlist.infra.exception.BadRequestException;
import com.wishlist.infra.exception.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public WishlistItem getWishlistItemById(final Long id) {
        return wishlistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item não encontrado"));
    }

    public void addWishlist(final Wishlist wishlist){
        wishlist.getItens().forEach(this::addWishlistItem);
    }

    public WishlistItem addWishlistItem(WishlistItem wishlistItem){
        final List<WishlistItem> wishlist = wishlistRepository.findWishlistItemByClientId(wishlistItem.getClientId());
        if (wishlist.size() >= 20) {
            throw new BadRequestException("Cliente já possui 20 itens em sua wishlist");
        }
        boolean itemAlreadyOnWishlist = wishlist.stream()
                .anyMatch(existentWishlistItem ->
                        Objects.equals(existentWishlistItem.getProductId(), wishlistItem.getProductId()));
        if (itemAlreadyOnWishlist) {
            throw new BadRequestException("Cliente já possui esse item em sua wishlist");
        }
        wishlistItem.setId(sequenceGeneratorService.generateSequence(WishlistItem.SEQUENCE_NAME));
        return wishlistRepository.save(wishlistItem);
    }

    public Wishlist getWishlistByClientId(final Integer clientId){
        final List<WishlistItem> wishlistItens = wishlistRepository.findWishlistItemByClientId(clientId);
        if (wishlistItens.isEmpty()) {
            throw new NotFoundException("Cliente não possui itens em sua wishlist");
        }
        Wishlist wishlist = new Wishlist();
        wishlist.setItens(wishlistItens);
        return wishlist;
    }

    public WishlistItem getWishlistByClientIdAndProductId(final Integer clientId, final Integer productId) {
        final List<WishlistItem> wishlistItens = wishlistRepository.findWishlistItemByClientIdAndProductId(clientId, productId);
        if (wishlistItens.isEmpty()) {
            throw new NotFoundException("Cliente não possui esse item em sua wishlist");
        }
        return wishlistItens.get(0);
    }

    public void deleteWishlistItemById(final Long id) {
        wishlistRepository.deleteById(id);
    }

    public void deleteWishlist() {
        wishlistRepository.deleteAll();
    }
}