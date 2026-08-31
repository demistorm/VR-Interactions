package win.demistorm.vr_interactions.client.interaction;

// Everything interaction processing needs for a tick (hand info is never null, missing pose data skips the tick)
public final class TickContext {

    private final long tick;
    private final HandInfo mainHand;
    private final HandInfo offHand;
    private final HeadInfo head;
    private final boolean interactDownMain;
    private final boolean interactDownOff;
    private final Haptics haptics;

    public TickContext(long tick, HandInfo mainHand, HandInfo offHand, HeadInfo head,
                       boolean interactDownMain, boolean interactDownOff, Haptics haptics) {
        this.tick = tick;
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.head = head;
        this.interactDownMain = interactDownMain;
        this.interactDownOff = interactDownOff;
        this.haptics = haptics;
    }

    public long tick() {
        return tick;
    }

    public HandInfo hand(Hand hand) {
        return hand == Hand.MAIN ? mainHand : offHand;
    }

    public HeadInfo head() {
        return head;
    }

    // Whether the interact key is held for this hand
    public boolean interactDown(Hand hand) {
        return hand == Hand.MAIN ? interactDownMain : interactDownOff;
    }

    public Haptics haptics() {
        return haptics;
    }
}
