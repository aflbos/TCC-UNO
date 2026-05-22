package uno.game.models;

public class Card {
    private Color color;
    private Value value;

    public Card(Color color, Value value) {
        this.color = color;
        this.value = value;
    }

    public Card(int id) {
        setId(id);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public int getId() {
        switch (value) {
            case WILD:
                return 52;
            case DRAW_FOUR:
                return 53;
            default:
                return color.ordinal() * 13 + value.ordinal();
        }
    }

    public void setId(int id) {
        if (id == 52) {
            this.color = Color.BLACK;
            this.value = Value.WILD;
        } else if (id == 53) {
            this.color = Color.BLACK;
            this.value = Value.DRAW_FOUR;
        } else {
            int colorIndex = id / 13;
            int valueIndex = id % 13;
            this.color = Color.values()[colorIndex];
            this.value = Value.values()[valueIndex];
        }
    }

    @Override
    public String toString() {
        return color + " " + value;
    }
}
