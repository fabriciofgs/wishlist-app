package com.wishlist;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class WishlistApplicationTests {

	@Test
	void contextLoads() {
	}

    @Test
    void mainDoesNotThrow() {
        assertDoesNotThrow(() -> WishlistApplication.main(new String[]{}));
    }

}
