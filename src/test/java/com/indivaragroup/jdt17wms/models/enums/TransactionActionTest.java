package com.indivaragroup.jdt17wms.models.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionActionTest {

    @Test
    void fromString_shouldReturnNull_whenInputIsNull() {
        assertNull(TransactionAction.fromString(null));
    }

    @Test
    void fromString_shouldReturnEnum_whenValidInput() {
        assertEquals(TransactionAction.BUY, TransactionAction.fromString("BUY"));
        assertEquals(TransactionAction.BUY, TransactionAction.fromString("buy"));
        assertEquals(TransactionAction.BUY, TransactionAction.fromString("Buy"));

        assertEquals(TransactionAction.SELL, TransactionAction.fromString("SELL"));
        assertEquals(TransactionAction.SELL, TransactionAction.fromString("sell"));
        assertEquals(TransactionAction.SELL, TransactionAction.fromString("Sell"));
    }

    @Test
    void fromString_shouldReturnNull_whenInvalidInput() {
        assertNull(TransactionAction.fromString("INVALID"));
        assertNull(TransactionAction.fromString(""));
    }

    @Test
    void testEnumValuesAndValueOf() {
        assertEquals(2, TransactionAction.values().length);
        assertEquals(TransactionAction.BUY, TransactionAction.valueOf("BUY"));
        assertEquals(TransactionAction.SELL, TransactionAction.valueOf("SELL"));
    }
}
