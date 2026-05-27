package uno.game.models;

public enum Color {
    RED, GREEN, BLUE, YELLOW, BLACK;

    @Override
    public String toString() {
        switch (this) {
            case RED: return "VERMELHO";
            case GREEN: return "VERDE";
            case BLUE: return "AZUL";
            case YELLOW: return "AMARELO";
            case BLACK: return "PRETO";
            default: return name();
        }
    }
}
