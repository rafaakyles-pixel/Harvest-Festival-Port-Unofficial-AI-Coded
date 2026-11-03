package harvestfestival.calendar;

import java.util.Locale;

public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}