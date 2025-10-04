package com.wishlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishlist.infra.exception.RestExceptionHandler;
import com.wishlist.domain.model.Wishlist;
import com.wishlist.domain.model.WishlistItem;
import com.wishlist.infra.exception.NotFoundException;
import com.wishlist.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistController wishlistController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // registra também o ControllerAdvice para que exceções lancadas pelo service sejam convertidas em responses
        mockMvc = MockMvcBuilders.standaloneSetup(wishlistController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
        objectMapper.findAndRegisterModules(); // registra módulo JavaTime para LocalDate
    }

    private WishlistItem sampleItem() {
        WishlistItem it = new WishlistItem();
        it.setId(1L);
        it.setClientId(10);
        it.setProductId(100);
        it.setProductName("Produto X");
        it.setClientName("Cliente Y");
        it.setDate(LocalDate.of(2023,1,1));
        return it;
    }

    private Wishlist sampleWishlist() {
        Wishlist w = new Wishlist();
        w.setItens(List.of(sampleItem()));
        return w;
    }

    @Test
    void getWishlistItemById_returnsItem() throws Exception {
        WishlistItem it = sampleItem();
        when(wishlistService.getWishlistItemById(1L)).thenReturn(it);

        mockMvc.perform(get("/wishlist/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(it)));

        verify(wishlistService).getWishlistItemById(1L);
    }

    @Test
    void getWishlistbyClientId_returnsWishlist() throws Exception {
        Wishlist w = sampleWishlist();
        when(wishlistService.getWishlistByClientId(10)).thenReturn(w);

        mockMvc.perform(get("/wishlist/client/{clientId}", 10))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(w)));

        verify(wishlistService).getWishlistByClientId(10);
    }

    @Test
    void getWishlistByClientIdAndProductId_returnsItem() throws Exception {
        WishlistItem it = sampleItem();
        when(wishlistService.getWishlistByClientIdAndProductId(10, 100)).thenReturn(it);

        mockMvc.perform(get("/wishlist/client/{clientId}/product/{productId}", 10, 100))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(it)));

        verify(wishlistService).getWishlistByClientIdAndProductId(10, 100);
    }

    @Test
    void addWishlistItem_returnsSavedItem() throws Exception {
        WishlistItem toSave = sampleItem();
        toSave.setId(null); // simula payload sem id
        WishlistItem saved = sampleItem();
        when(wishlistService.addWishlistItem(any(WishlistItem.class))).thenReturn(saved);

        mockMvc.perform(post("/wishlist/add_item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toSave)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(saved)));

        verify(wishlistService).addWishlistItem(any(WishlistItem.class));
    }

    @Test
    void addWishlist_returnsSuccessMessage() throws Exception {
        Wishlist w = sampleWishlist();
        doNothing().when(wishlistService).addWishlist(any(Wishlist.class));

        mockMvc.perform(post("/wishlist/add_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w)))
                .andExpect(status().isOk())
                .andExpect(content().string("Itens adicionados a wishlist com sucesso"));

        verify(wishlistService).addWishlist(any(Wishlist.class));
    }

    @Test
    void deleteWishlistItemById_returnsSuccessMessage() throws Exception {
        doNothing().when(wishlistService).deleteWishlistItemById(1L);

        mockMvc.perform(delete("/wishlist/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Item removido com sucesso"));

        verify(wishlistService).deleteWishlistItemById(1L);
    }

    @Test
    void deleteWishlist_returnsSuccessMessage() throws Exception {
        doNothing().when(wishlistService).deleteWishlist();

        mockMvc.perform(delete("/wishlist/delete"))
                .andExpect(status().isOk())
                .andExpect(content().string("Todos os items foram removidos da wishlist com sucesso"));

        verify(wishlistService).deleteWishlist();
    }

    @Test
    void getWishlistItemById_whenNotFound_serviceThrowsNotFoundException_mapsTo404() throws Exception {
        when(wishlistService.getWishlistItemById(999L)).thenThrow(new NotFoundException("Item não encontrado"));

        mockMvc.perform(get("/wishlist/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Item não encontrado"));

        verify(wishlistService).getWishlistItemById(999L);
    }
}
