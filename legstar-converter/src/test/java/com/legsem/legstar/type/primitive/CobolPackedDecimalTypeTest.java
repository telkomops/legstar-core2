package com.legsem.legstar.type.primitive;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import com.legsem.legstar.context.CobolContext;
import com.legsem.legstar.context.EbcdicCobolContext;
import com.legsem.legstar.type.primitive.CobolPackedDecimalType;
import com.legsem.legstar.utils.HexUtils;

public class CobolPackedDecimalTypeTest {

    private CobolContext cobolContext;

    @Before
    public void setUp() {
        cobolContext = new EbcdicCobolContext();
    }

    @Test
    public void testConstructor() {
        try {
            new CobolPackedDecimalType.Builder < BigDecimal >(cobolContext,
                    BigDecimal.class).signed(true).totalDigits(-1)
                    .fractionDigits(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Total digits number -1 cannot be negative",
                    e.getMessage());
        }
        try {
            new CobolPackedDecimalType.Builder < BigDecimal >(cobolContext,
                    BigDecimal.class).signed(true).totalDigits(0)
                    .fractionDigits(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Total digits cannot be zero", e.getMessage());
        }
        try {
            new CobolPackedDecimalType.Builder < BigDecimal >(cobolContext,
                    BigDecimal.class).signed(true).totalDigits(1)
                    .fractionDigits(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Fraction digits number -1 cannot be negative",
                    e.getMessage());
        }
        try {
            new CobolPackedDecimalType.Builder < BigDecimal >(cobolContext,
                    BigDecimal.class).signed(true).totalDigits(45)
                    .fractionDigits(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Total digits number 45 cannot exceed 31",
                    e.getMessage());
        }
        try {
            new CobolPackedDecimalType.Builder < BigDecimal >(cobolContext,
                    BigDecimal.class).signed(true).totalDigits(2)
                    .fractionDigits(3).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "Fraction digits number 3 cannot exceed total digits number 2",
                    e.getMessage());
        }
    }

    @Test
    public void testBytesLength() {
        assertEquals(1, new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(true).totalDigits(1)
                .fractionDigits(0).build().getBytesLen());
        assertEquals(2, new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(true).totalDigits(2)
                .fractionDigits(0).build().getBytesLen());
        assertEquals(3, new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(true).totalDigits(5)
                .fractionDigits(0).build().getBytesLen());
        assertEquals(4, new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(true).totalDigits(6)
                .fractionDigits(0).build().getBytesLen());
        assertEquals(4, new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(true).totalDigits(7)
                .fractionDigits(0).build().getBytesLen());
    }

    @Test
    public void testIsValidTrueCases() {
        assertTrue(isValid(false, 1, 0, "1F"));
        assertTrue(isValid(true, 5, 2, "42567C"));
        assertTrue(isValid(false, 6, 5, "0012345F"));
        assertTrue(isValid(false, 31, 5, "1234567890123456789012345678901F"));
    }

    @Test
    public void testIsValidFalseLengthCases() {
        assertFalse(isValid(false, 2, 0, "1F"));
    }

    @Test
    public void testIsValidFalseNonDigitsCases() {
        assertFalse(isValid(false, 1, 0, "AF"));
        assertFalse(isValid(true, 5, 2, "0A1C"));
    }

    @Test
    public void testIsValidFalseSignCases() {
        assertFalse(isValid(true, 5, 2, "0310"));
        assertFalse(isValid(true, 5, 2, "031F"));
        assertFalse(isValid(false, 5, 2, "031C"));
        assertFalse(isValid(false, 5, 2, "031D"));
    }

    @Test
    public void testFromHostDecimal() {
        assertEquals("456790.00675", getValue(false, 11, 5, "45679000675f"));
        assertEquals("-123456789012345.12",
                getValue(true, 17, 2, "12345678901234512d"));
        assertEquals("0.00", getValue(true, 17, 2, "00000000000000000c"));
        assertEquals("-67000.56007", getValue(true, 10, 5, "06700056007d"));
        assertEquals("-67000560.07", getValue(true, 10, 2, "06700056007d"));
        assertEquals("1", getValue(false, 1, 0, "1f"));
    }

    @Test
    public void testFromHostDecimalVariousJavaTypes() {
        try {
            getValue(Short.class, false, 11, 5, "45679000675f");
            fail();
        } catch (Exception e) {
            assertEquals(
                    "Host 6 bytes numeric converts to '456790.00675' which is not a valid java.lang.Short." +
                    " Position is 0." +
                    " Data at position 0x->45679000675F",
                    e.getMessage());
        }
        assertEquals("456790",
                getValue(Integer.class, false, 11, 5, "45679000675f"));
        assertEquals("456790",
                getValue(Long.class, false, 11, 5, "45679000675f"));
        assertEquals("456790",
                getValue(BigInteger.class, false, 11, 5, "45679000675f"));
        assertEquals("456790.00675",
                getValue(BigDecimal.class, false, 11, 5, "45679000675f"));

    }

    private boolean isValid(boolean signed, int totalDigits,
            int fractionDigits, String hexHostData) {

        CobolPackedDecimalType < BigDecimal > type = new CobolPackedDecimalType.Builder < BigDecimal >(
                cobolContext, BigDecimal.class).signed(signed)
                .totalDigits(totalDigits).fractionDigits(fractionDigits)
                .build();

        return type.isValid(HexUtils.decodeHex(hexHostData), 0);
    }

    private String getValue(boolean signed, int totalDigits,
            int fractionDigits, String hexHostData) {
        return getValue(BigDecimal.class, signed, totalDigits, fractionDigits,
                hexHostData);
    }

    private <T extends Number> String getValue(Class < T > clazz,
            boolean signed, int totalDigits, int fractionDigits,
            String hexHostData) {

        CobolPackedDecimalType < T > type = new CobolPackedDecimalType.Builder < T >(
                cobolContext, clazz).signed(signed).totalDigits(totalDigits)
                .fractionDigits(fractionDigits).build();

        return type.fromHost(HexUtils.decodeHex(hexHostData), 0).getValue()
                .toString();

    }
}