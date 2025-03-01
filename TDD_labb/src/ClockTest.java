import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClockTest {

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = new Clock();
    }

    // test för valid transitions

    @Test
    void fromDisplayTimeToDisplayDate() {
        assertEquals("Switched to DISPLAY_DATE mode. Current date: 2000-01-01", clock.changeMode());
    }

    @Test
    void fromDisplayDateToDisplayTime() {
        clock.changeMode(); // DISPLAY_DATE
        assertEquals("Switched to DISPLAY_TIME mode. Current time: 00:00:00", clock.changeMode());
    }

    @Test
    void fromDisplayTimeToChangeTime() {
        assertEquals("Ready to set time.", clock.ready());
    }

    @Test
    void fromChangeTimeToDisplayTime() {
        clock.ready(); // CHANGE_TIME
        assertEquals("10:00:00", clock.set(10, 0, 0));
        assertEquals("Exiting time change mode: 10:00:00", clock.changeMode()); // gå till display time igen

    }

    @Test
    void fromDisplayDateToChangeDate() {
        clock.changeMode(); // DISPLAY_DATE
        assertEquals("Ready to set date.", clock.ready());
    }

    @Test
    void fromChangeDateToDisplayDate() {
        clock.changeMode(); // DISPLAY_DATE
        clock.ready(); // CHANGE_DATE
        assertEquals("2024-11-11", clock.set(2024, 11, 11));
        assertEquals("Exiting date change mode: 2024-11-11", clock.changeMode());

    }

    // test för invalid transitions

    @Test
    void cannotSetTimeInDisplayTime() {
        assertEquals("Set operation not allowed in current state.", clock.set(10, 0, 0));
    }

    @Test
    void cannotSetTimeInDisplayDate() {
        clock.changeMode();
        assertEquals("Set operation not allowed in current state.", clock.set(10, 0, 0));
    }

    @Test
    void cannotSetDateInDisplayDate() {
        clock.changeMode(); // DISPLAY_DATE
        assertEquals("Set operation not allowed in current state.", clock.set(2020, 11, 11));
    }

    @Test
    void cannotSetDateInDisplayTime() {
        assertEquals("Set operation not allowed in current state.", clock.set(2020, 11, 11));
    }

    @Test
    void cannotReadyInChangeTime() {
        clock.ready(); // CHANGE_TIME
        assertEquals("Operation not allowed in the current state.", clock.ready());
    }

    @Test
    void cannotReadyInChangeDate() {
        clock.changeMode(); // DISPLAY_DATE
        clock.ready(); // CHANGE_DATE
        assertEquals("Operation not allowed in the current state.", clock.ready());
    }


    // BVA tester

    @Test
    void setValidHourLowerBound() {
        clock.ready(); // CHANGE_TIME
        assertEquals("00:00:00", clock.set(0, 0, 0));
    }

    @Test
    void setInvalidHourBelowLowerBound() {
        clock.ready();
        assertEquals("Invalid hour, cannot be less than 0.", clock.set(-1, 0, 0));
    }

    @Test
    void setValidHourUpperBound() {
        clock.ready();
        assertEquals("23:00:00", clock.set(23, 0, 0));
    }

    @Test
    void setInvalidHourAboveUpperBound() {
        clock.ready();
        assertEquals("Invalid hour, cannot be greater than 23.", clock.set(24, 0, 0));
    }

    @Test
    void setValidMinuteLowerBound() {
        clock.ready(); // CHANGE_TIME
        assertEquals("00:00:00", clock.set(0, 0, 0));
    }

    @Test
    void setInvalidMinuteBelowLowerBound() {
        clock.ready();
        assertEquals("Invalid minute, cannot be less than 0.", clock.set(0, -1, 0));
    }

    @Test
    void setValidMinuteUpperBound() {
        clock.ready();
        assertEquals("00:59:00", clock.set(0, 59, 0));
    }

    @Test
    void setInvalidMinuteAboveUpperBound() {
        clock.ready();
        assertEquals("Invalid minute, cannot be greater than 59.", clock.set(0, 60, 0));
    }

    @Test
    void setValidSecondLowerBound() {
        clock.ready();
        assertEquals("00:00:00", clock.set(0, 0, 0));
    }

    @Test
    void setInvalidSecondBelowLowerBound() {
        clock.ready();
        assertEquals("Invalid second, cannot be less than 0.", clock.set(0, 0, -1));
    }

    @Test
    void setValidSecondUpperBound() {
        clock.ready();
        assertEquals("00:00:59", clock.set(0, 0, 59));
    }

    @Test
    void setInvalidSecondAboveUpperBound() {
        clock.ready();
        assertEquals("Invalid second, cannot be greater than 59.", clock.set(0, 0, 60));
    }

    @Test
    void setValidYearLowerBound() {
        clock.changeMode(); // DISPLAY_DATE
        clock.ready(); // CHANGE_DATE
        assertEquals("2000-01-01", clock.set(2000, 1, 1));
    }

    @Test
    void setInvalidYearBelowLowerBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid year, cannot be less than 2000.", clock.set(1999, 1, 1));
    }

    @Test
    void setValidYearUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("2100-01-01", clock.set(2100, 1, 1));
    }

    @Test
    void setInvalidYearAboveUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid year, cannot be greater than 2100.", clock.set(2101, 1, 1));
    }

    @Test
    void setValidMonthLowerBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("2000-01-01", clock.set(2000, 1, 1));
    }

    @Test
    void setInvalidMonthBelowLowerBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid month, cannot be less than 1.", clock.set(2000, 0, 1));
    }

    @Test
    void setValidMonthUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("2000-12-01", clock.set(2000, 12, 1));
    }

    @Test
    void setInvalidMonthAboveUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid month, cannot be greater than 12.", clock.set(2000, 13, 1));
    }

    @Test
    void setValidDayLowerBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("2000-01-01", clock.set(2000, 1, 1));
    }

    @Test
    void setInvalidDayBelowLowerBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid day, cannot be less than 1.", clock.set(2000, 1, 0));
    }

    @Test
    void setValidDayUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("2000-01-31", clock.set(2000, 1, 31));
    }

    @Test
    void setInvalidDayAboveUpperBound() {
        clock.changeMode();
        clock.ready();
        assertEquals("Invalid day, cannot be greater than 31.", clock.set(2000, 1, 32));
    }

}
