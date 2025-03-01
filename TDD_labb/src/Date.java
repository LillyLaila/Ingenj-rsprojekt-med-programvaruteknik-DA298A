public class Date {

    private int theYear = 2000;
    private int theMonth = 1;
    private int theDay = 1;

    protected String dateSet(int year, int month, int day){
        if (year < 2000) {
            return "Invalid year, cannot be less than 2000.";
        }
        if (year > 2100) {
            return "Invalid year, cannot be greater than 2100.";
        }
        if (month < 1) {
            return "Invalid month, cannot be less than 1.";
        }
        if (month > 12) {
            return "Invalid month, cannot be greater than 12.";
        }
        if (day < 1) {
            return "Invalid day, cannot be less than 1.";
        }
        if (day > 31) {
            return "Invalid day, cannot be greater than 31.";
        }
        this.theYear = year;
        this.theMonth = month;
        this.theDay = day;
        return showDate();
    }



    protected String showDate() {
        return String.format("%04d-%02d-%02d", theYear, theMonth, theDay);
    }
}
