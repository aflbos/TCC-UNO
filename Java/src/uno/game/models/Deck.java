package uno.game.models;

import java.util.ArrayList;
import java.util.Random;

public class Deck {
    private ArrayList<Card> cards;

    public Deck() {
        ArrayList<Card> cards = new ArrayList<>();
        Color[] colors = Color.values();
        Value[] values = Value.values();

        // Add number cards
        for (int i = 0; i < 4; i++) {
            for (int j = 1; j < 20; j++) {
                cards.add(new Card(colors[i], values[j/2]));
            }
        }

        // Add action cards
        for (int n = 0; n < 2; n++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 10; j < 13; j++) {
                    cards.add(new Card(colors[i], values[j]));
                }
            }
        }

        // Add wild cards
        for (int n = 0; n < 4; n++) {
            cards.add(new Card(Color.BLACK, Value.WILD));
            cards.add(new Card(Color.BLACK, Value.DRAW_FOUR));
        }

        this.cards = cards;
    }

    public void shuffle(int seed) {
        Random random;

        if (seed == -1) {
            random = new Random();
        } else {
            random = new Random(seed);
        }

        for (int i = cards.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Card temp = cards.get(i);
            cards.set(i, cards.get(j));
            cards.set(j, temp);
        }
    }

    public Card drawCard() {
        if (!cards.isEmpty()) {
            return cards.remove(cards.size() - 1);
        }

        return null; // No cards left
    }

    public void placeCard(Card card) {
        cards.add(card);
    }

    public void emptyDeck() {
        cards.clear();
    }

    public Card peekTopCard() {
        if (!cards.isEmpty()) {
            return cards.get(cards.size() - 1);
        }

        return null; // No cards left
    }

    public Card peekBottomCard() {
        if (!cards.isEmpty()) {
            return cards.get(0);
        }

        return null; // No cards left
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
    }

    @Override
    public String toString() {
        if (cards.isEmpty()) {
            return "No cards in deck.";
        }

        return cards.get(cards.size() - 1).toString();
    }
}
