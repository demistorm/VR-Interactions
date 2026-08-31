package win.demistorm.vr_interactions.client.interaction;

// Which controller, MAIN is the pointing hand (using own enum to hopefully make mutliversion support simpler)
public enum Hand {
    MAIN,
    OFF;

    public Hand opposite() {
        return this == MAIN ? OFF : MAIN;
    }
}
