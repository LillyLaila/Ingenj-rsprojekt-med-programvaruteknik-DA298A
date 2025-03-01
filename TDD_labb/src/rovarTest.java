import static org.junit.jupiter.api.Assertions.*;

class rovarTest {

    @org.junit.jupiter.api.Test
    void enrov() {
        assertEquals("hohejoj", rovar.enrov("hej"));
        assertEquals("HOHejoj", rovar.enrov("Hej"));
        assertEquals("", rovar.enrov(""));
        assertEquals("åäö", rovar.enrov("åäö"));
        assertEquals("ÅÄÖ", rovar.enrov("ÅÄÖ"));
        assertEquals("0123456789", rovar.enrov("0123456789"));
        assertEquals("!#€%&/", rovar.enrov("!#€%&/"));
        assertNull((rovar.enrov(null)));
    }

    @org.junit.jupiter.api.Test
    void derov() {
        assertEquals("hej", rovar.derov("hohejoj"));
        assertEquals("Hej", rovar.derov("HOHejoj"));
        assertEquals(" ", rovar.derov(" "));
        assertEquals("åäö", rovar.derov("åäö"));
        assertEquals("ÅÄÖ", rovar.derov("ÅÄÖ"));
        assertEquals("0123456789", rovar.derov("0123456789"));
        assertEquals("!#€%&/", rovar.derov("!#€%&/"));
        assertNull((rovar.derov(null)));
    }

}