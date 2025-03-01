public class Clock {

    private Time theTime = new Time();
    private Date theDate = new Date();
    private State currentState = State.DISPLAY_TIME;

    enum State {
        DISPLAY_TIME,
        DISPLAY_DATE,
        CHANGE_TIME,
        CHANGE_DATE
    }

    /**
     * This method changes the mode.
     *
     * @return a message indicating the new mode or an error message if the operation is not allowed
     */
    public String changeMode() {
        switch (currentState) {
            case DISPLAY_TIME:
                currentState = State.DISPLAY_DATE;
                return "Switched to DISPLAY_DATE mode. Current date: " + theDate.showDate();
            case DISPLAY_DATE:
                currentState = State.DISPLAY_TIME;
                return "Switched to DISPLAY_TIME mode. Current time: " + theTime.showTime();
            case CHANGE_TIME:
                currentState = State.CHANGE_DATE;
                return "Exiting time change mode: " + theTime.showTime();
            case CHANGE_DATE:
                currentState = State.CHANGE_TIME;
                return "Exiting date change mode: " + theDate.showDate();
            default:
                return "Unknown state";
        }
    }


    /**
     * This method checks if it's ready to set the date or time.
     *
     * @return "yes" if it's ready to change time/date, otherwise "no"
     */
    public String ready() {
        switch (currentState) {
            case DISPLAY_TIME:
                currentState = State.CHANGE_TIME;
                return "Ready to set time.";
            case DISPLAY_DATE:
                currentState = State.CHANGE_DATE;
                return "Ready to set date.";
            default:
                return "Operation not allowed in the current state.";
        }
    }


    /**
     * This method sets the date or time.
     *
     * @param p1 Year or Hour
     * @param p2 Month or Minute
     * @param p3 Day or Second
     * @return Updated date or time as a string
     */
    public String set(int p1, int p2, int p3) {
        switch (currentState) {
            case CHANGE_TIME:
                return theTime.timeSet(p1, p2, p3);
            case CHANGE_DATE:
                return theDate.dateSet(p1, p2, p3);
            default:
                return "Set operation not allowed in current state.";
        }
    }
}

