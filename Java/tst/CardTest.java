import uno.game.models.Card;
import uno.game.models.Color;
import uno.game.models.Value;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @org.junit.jupiter.api.Test
    void getId() {
        ArrayList<Card> cards = new ArrayList<>();
        Color[] colors = Color.values();
        Value[] values = Value.values();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 13; j++) {
                cards.add(new Card(colors[i], values[j]));
            }
        }

        for (int n = 0; n < 1; n++) {
            cards.add(new Card(Color.BLACK, Value.WILD));
            cards.add(new Card(Color.BLACK, Value.DRAW_FOUR));
        }

        for (int i = 0; i < cards.size(); i++) {
            assertEquals(i, cards.get(i).getId());
        }
    }
}
