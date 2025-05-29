package com.example.crm1;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestUnit2 {

    @Test
    public void testPasswordValidation() {

        assertTrue(RegisterActivity.isValidPassword("password1"));
        assertTrue(RegisterActivity.isValidPassword("longpassword123"));
        assertTrue(RegisterActivity.isValidPassword("12345678"));


        assertFalse(RegisterActivity.isValidPassword("short"));
        assertFalse(RegisterActivity.isValidPassword(""));
        assertFalse(RegisterActivity.isValidPassword(null));
    }
}
