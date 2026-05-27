package uno.game.models;

public enum Value {
    ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
    SKIP, REVERSE, DRAW_TWO,
    WILD, DRAW_FOUR;

    @Override
    public String toString() {
        switch (this) {
            case ZERO: return "0";
            case ONE: return "1";
            case TWO: return "2";
            case THREE: return "3";
            case FOUR: return "4";
            case FIVE: return "5";
            case SIX: return "6";
            case SEVEN: return "7";
            case EIGHT: return "8";
            case NINE: return "9";
            case SKIP: return "PULAR";
            case REVERSE: return "INVERTER";
            case DRAW_TWO: return "COMPRA 2";
            case WILD: return "CURINGA";
            case DRAW_FOUR: return "COMPRA 4";
            default: return name();
        }
    }
}
