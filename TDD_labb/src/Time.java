public class Time {

    private int theHour = 0;
    private int theMinute = 0;
    private int theSecond = 0;

    protected String timeSet(int hour, int minute, int second) {
        if (hour < 0) {
            return "Invalid hour, cannot be less than 0.";
        }
        if (hour > 23) {
            return "Invalid hour, cannot be greater than 23.";
        }
        if (minute < 0 ) {
            return "Invalid minute, cannot be less than 0.";
        }
        if (minute > 59) {
            return "Invalid minute, cannot be greater than 59.";
        }
        if (second < 0) {
            return "Invalid second, cannot be less than 0.";
        }
        if (second > 59) {
            return "Invalid second, cannot be greater than 59.";
        }
        this.theHour = hour;
        this.theMinute = minute;
        this.theSecond = second;
        return showTime();
    }

    protected String showTime() {
        return String.format("%02d:%02d:%02d", theHour, theMinute, theSecond);
    }
}
