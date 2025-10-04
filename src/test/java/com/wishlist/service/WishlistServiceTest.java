package com.wishlist.service;

import com.wishlist.domain.model.Wishlist;
import com.wishlist.domain.model.WishlistItem;
import com.wishlist.domain.repository.WishlistRepository;
import com.wishlist.infra.exception.BadRequestException;
import com.wishlist.infra.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private SequenceGeneratorService sequenceGeneratorService;

    @InjectMocks
    private WishlistService wishlistService;

    @Captor
    private ArgumentCaptor<WishlistItem> wishlistItemCaptor;

    private WishlistItem sampleItem() {
        WishlistItem it = new WishlistItem();
        it.setId(1L);
        it.setClientId(10);
        it.setProductId(100);
        it.setProductName("Produto X");
        it.setClientName("Cliente Y");
        return it;
    }

    private Wishlist sampleWishlist() {
        Wishlist w = new Wishlist();
        w.setItens(List.of(sampleItem()));
        return w;
    }

    @Nested
    @DisplayName("getWishlistItemById")
    class GetById {
        @Test
        void returnsItemWhenFound() {
            WishlistItem it = sampleItem();
            when(wishlistRepository.findById(1L)).thenReturn(Optional.of(it));

            WishlistItem result = wishlistService.getWishlistItemById(1L);

            assertThat(result).isSameAs(it);
            verify(wishlistRepository).findById(1L);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(wishlistRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.getWishlistItemById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Item não encontrado");

            verify(wishlistRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("addWishlist (batch)")
    class AddWishlistBatch {
        @Test
        void callsAddWishlistItemForEachItemAndSaves() {
            Wishlist w = new Wishlist();
            List<WishlistItem> items = IntStream.range(0, 3)
                    .mapToObj(i -> {
                        WishlistItem it = new WishlistItem();
                        it.setClientId(1);
                        it.setProductId(100 + i);
                        return it;
                    }).collect(Collectors.toList());
            w.setItens(items);

            when(wishlistRepository.findWishlistItemByClientId(1)).thenReturn(List.of());
            when(sequenceGeneratorService.generateSequence(WishlistItem.SEQUENCE_NAME)).thenReturn(100L);
            when(wishlistRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            wishlistService.addWishlist(w);

            verify(wishlistRepository, times(3)).save(any(WishlistItem.class));
            verify(sequenceGeneratorService, times(3)).generateSequence(WishlistItem.SEQUENCE_NAME);
        }
    }

    @Nested
    @DisplayName("addWishlistItem")
    class AddSingle {
        @Test
        void throwsBadRequestWhenClientHas20Items() {
            WishlistItem toAdd = sampleItem();
            List<WishlistItem> twenty = IntStream.range(0, 20)
                    .mapToObj(i -> {
                        WishlistItem it = new WishlistItem();
                        it.setClientId(toAdd.getClientId());
                        it.setProductId(1000 + i);
                        return it;
                    }).collect(Collectors.toList());

            when(wishlistRepository.findWishlistItemByClientId(toAdd.getClientId())).thenReturn(twenty);

            assertThatThrownBy(() -> wishlistService.addWishlistItem(toAdd))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cliente já possui 20 itens em sua wishlist");

            verify(wishlistRepository, never()).save(any());
            verify(sequenceGeneratorService, never()).generateSequence(anyString());
        }

        @Test
        void throwsBadRequestWhenItemAlreadyExists() {
            WishlistItem toAdd = sampleItem();
            WishlistItem existing = new WishlistItem();
            existing.setClientId(toAdd.getClientId());
            existing.setProductId(toAdd.getProductId());

            when(wishlistRepository.findWishlistItemByClientId(toAdd.getClientId())).thenReturn(List.of(existing));

            assertThatThrownBy(() -> wishlistService.addWishlistItem(toAdd))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cliente já possui esse item em sua wishlist");

            verify(wishlistRepository, never()).save(any());
            verify(sequenceGeneratorService, never()).generateSequence(anyString());
        }

        @Test
        void savesAndReturnsItemWhenValid() {
            WishlistItem toAdd = sampleItem();
            toAdd.setId(null);
            when(wishlistRepository.findWishlistItemByClientId(toAdd.getClientId())).thenReturn(List.of());
            when(sequenceGeneratorService.generateSequence(WishlistItem.SEQUENCE_NAME)).thenReturn(500L);
            when(wishlistRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WishlistItem saved = wishlistService.addWishlistItem(toAdd);

            verify(sequenceGeneratorService).generateSequence(WishlistItem.SEQUENCE_NAME);
            verify(wishlistRepository).save(wishlistItemCaptor.capture());

            WishlistItem captured = wishlistItemCaptor.getValue();
            assertThat(captured.getId()).isEqualTo(500L);
            assertThat(saved.getId()).isEqualTo(500L);
            assertThat(saved.getClientId()).isEqualTo(toAdd.getClientId());
            assertThat(saved.getProductId()).isEqualTo(toAdd.getProductId());
        }
    }

    @Nested
    @DisplayName("getWishlistByClientId")
    class GetByClient {
        @Test
        void throwsNotFoundWhenEmpty() {
            when(wishlistRepository.findWishlistItemByClientId(10)).thenReturn(List.of());

            assertThatThrownBy(() -> wishlistService.getWishlistByClientId(10))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Cliente não possui itens em sua wishlist");

            verify(wishlistRepository).findWishlistItemByClientId(10);
        }

        @Test
        void returnsWishlistWhenExists() {
            WishlistItem it = sampleItem();
            when(wishlistRepository.findWishlistItemByClientId(10)).thenReturn(List.of(it));

            Wishlist result = wishlistService.getWishlistByClientId(10);

            assertThat(result).isNotNull();
            assertThat(result.getItens()).containsExactly(it);
            verify(wishlistRepository).findWishlistItemByClientId(10);
        }
    }

    @Nested
    @DisplayName("getWishlistByClientIdAndProductId")
    class GetByClientAndProduct {
        @Test
        void throwsNotFoundWhenMissing() {
            when(wishlistRepository.findWishlistItemByClientIdAndProductId(10, 200)).thenReturn(List.of());

            assertThatThrownBy(() -> wishlistService.getWishlistByClientIdAndProductId(10, 200))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Cliente não possui esse item em sua wishlist");

            verify(wishlistRepository).findWishlistItemByClientIdAndProductId(10, 200);
        }

        @Test
        void returnsItemWhenFound() {
            WishlistItem it = sampleItem();
            when(wishlistRepository.findWishlistItemByClientIdAndProductId(10, 100)).thenReturn(List.of(it));

            WishlistItem result = wishlistService.getWishlistByClientIdAndProductId(10, 100);

            assertThat(result).isSameAs(it);
            verify(wishlistRepository).findWishlistItemByClientIdAndProductId(10, 100);
        }
    }

    @Nested
    @DisplayName("delete operations")
    class DeleteOperations {
        @Test
        void deleteWishlistItemById_delegatesToRepository() {
            doNothing().when(wishlistRepository).deleteById(1L);

            wishlistService.deleteWishlistItemById(1L);

            verify(wishlistRepository).deleteById(1L);
        }

        @Test
        void deleteWishlist_delegatesToRepository() {
            doNothing().when(wishlistRepository).deleteAll();

            wishlistService.deleteWishlist();

            verify(wishlistRepository).deleteAll();
        }
    }
}
