package uno.game.players;

import uno.game.event.GameEventListener;
import uno.game.engine.Simulation;
import uno.game.models.Card;

import java.util.ArrayList;

public abstract class Player implements GameEventListener {
    private ArrayList<Card> cards;
    private String name;
    Simulation simulation;

    public Player(String name, Simulation simulation) {
        this.cards = new ArrayList<>();
        this.name = name;
        this.simulation = simulation;
    }

    public void addCardToHand(Card card) {
        cards.add(card);
    }

    public Card discardCard(int index) {
        if (index >= 0 && index < cards.size()) {
            return cards.remove(index);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public Card discardCardId(int id) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId() == id) {
                return cards.remove(i);
            }
        }

        throw new IndexOutOfBoundsException("Card with id " + id + " not found");
    }

    public Card discardCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }

        for (int i = 0; i < cards.size(); i++) {
            Card current = cards.get(i);
            if (current == card || current.getId() == card.getId() ||
                    (current.getColor() == card.getColor() && current.getValue() == card.getValue())) {
                return cards.remove(i);
            }
        }

        return card;
    }

    public Card peekCard(int index) {
        if (index >= 0 && index < cards.size()) {
            return cards.get(index);
        }

        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public void sortCards() {
        cards.sort((card1, card2) -> {
            if (card1.getColor() != card2.getColor()) {
                return card1.getColor().compareTo(card2.getColor());
            } else {
                return Integer.compare(card1.getValue().ordinal(), card2.getValue().ordinal());
            }
        });
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Simulation getSimulation() {
        return this.simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public abstract int getInput();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            sb.append((i + 1)).append(". ").append(cards.get(i).toString()).append("\n");
        }
        return sb.toString();
    }
}
