package uno.game.engine;

import uno.game.event.GameEventListener;
import uno.game.event.Spectator;
import uno.game.loggers.Logger;
import uno.game.players.Player;
import uno.game.players.PlayerAI;
import uno.game.players.PlayerHuman;
import uno.game.players.PlayerNetwork;
import uno.game.players.PlayerRandom;
import uno.game.models.Card;
import uno.game.models.Color;
import uno.game.models.Deck;
import uno.game.models.Value;
import uno.ai.network.ConnectionAI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public class Simulation {
    private final Player[] players;
    private final Deck drawDeck;
    private final Deck discardPile;
    private final ConnectionAI ConnectionAI;
    private boolean gameOver;
    private Player winner;
    private int turnCounter = 0;
    private double[] observationVector;
    private double[] decisionMask;
    private double[] previousPlayerHandObservation;
    private int[] validInputs;
    private int[] playableCardIdsForNetwork = new int[0];
    private volatile boolean networkBotFirstDecisionDelayDone = false;
    private String id;
    private int seed;
    private boolean debug = false;
    private final boolean[] rules;
    private int stackingAmount = 0;
    private boolean previousPlayerBluffed = false;

    private int currentPlayerIndex = 0;
    private int direction = 1; // +1 forward, -1 backward

    private Logger logger = null;
    private final ArrayList<Spectator> spectators = new ArrayList<>();

    private final boolean RULE_STACKING;
    private final boolean RULE_SKIP_N_FLIP;
    private final boolean RULE_SEVEN_ZERO;
    private final boolean RULE_FORCE_PLAY_DRAWN;
    private final boolean RULE_DRAW_AND_PLAY;
    private final boolean RULE_DRAW_TO_MATCH;
    private final boolean RULE_BLUFFING;
    private final boolean RULE_PLAY_IDENTICAL;

    private boolean human;
    private boolean ai;
    private boolean random;

    private static final int MASK_COLOR_RED = 55;
    private static final int MASK_COLOR_GREEN = 56;
    private static final int MASK_COLOR_BLUE = 57;
    private static final int MASK_COLOR_YELLOW = 58;
    private static final int MASK_BINARY_YES = 68;
    private static final int MASK_BINARY_NO = 69;

    private static final int OBS_SIZE = 189;
    private static final int FLAG_CHALLENGE_DRAW4 = OBS_SIZE - 1; // index 188
    private static final int FLAG_STACK = OBS_SIZE - 2; // index 187
    private static final int FLAG_PLAY_OR_DRAW = OBS_SIZE - 3; // index 186
    private static final int FLAG_PLAY_WHEN_NO_DRAW = OBS_SIZE - 4; // index 185
    private static final int FLAG_PLAY_DRAWN_CARD = OBS_SIZE - 5; // index 184
    private static final int FLAG_CHOOSE_CARD = OBS_SIZE - 6; // index 183
    private static final int FLAG_SWAP_HANDS = OBS_SIZE - 7; // index 182
    private static final int FLAG_CHOOSE_COLOR = OBS_SIZE - 8; // index 181
    private static final int FLAG_PLAY_IDENTICAL = OBS_SIZE - 9; // index 180

    public Simulation(int numHumanPlayers, int numAIPlayers, int numRandomPlayers, ConnectionAI ConnectionAI, boolean[] rules, String id, int seed) {
        Player[] players = new Player[numHumanPlayers + numAIPlayers + numRandomPlayers];
        this.drawDeck = new Deck();
        this.discardPile = new Deck();
        this.gameOver = false;
        this.ConnectionAI = ConnectionAI;
        this.id = id;
        this.seed = seed;
        this.rules = rules;

        drawDeck.shuffle(seed);

        for (int i = 0; i < numHumanPlayers; i++) {
            players[i] = new PlayerHuman("H. Player" + (i + 1), this);
            for (int j = 0; j < 7; j++) {
                players[i].addCardToHand(drawDeck.drawCard());
            }
            players[i].sortCards();
        }

        for (int i = numHumanPlayers; i < numHumanPlayers + numAIPlayers; i++) {
            players[i] = new PlayerAI("AI. Player " + (i + 1), this, ConnectionAI);
            for (int j = 0; j < 7; j++) {
                players[i].addCardToHand(drawDeck.drawCard());
            }
            players[i].sortCards();
        }

        for (int i = numHumanPlayers + numAIPlayers; i < numHumanPlayers + numAIPlayers + numRandomPlayers; i++) {
            players[i] = new PlayerRandom("R. Player " + (i + 1), this);
            for (int j = 0; j < 7; j++) {
                players[i].addCardToHand(drawDeck.drawCard());
            }
            players[i].sortCards();
        }

        this.players = players;

        this.RULE_STACKING = rules[0];
        this.RULE_SKIP_N_FLIP = rules[1];
        this.RULE_SEVEN_ZERO = rules[2];
        this.RULE_FORCE_PLAY_DRAWN = rules[3];
        this.RULE_DRAW_AND_PLAY = rules[4];
        this.RULE_DRAW_TO_MATCH = rules[5];
        this.RULE_BLUFFING = rules[6];
        this.RULE_PLAY_IDENTICAL = rules[7];

        initGame();
    }

    public Simulation(Player[] players, ConnectionAI ConnectionAI, boolean[] rules, String id, int seed) {
        this.players = players;
        this.drawDeck = new Deck();
        this.discardPile = new Deck();
        this.gameOver = false;
        this.ConnectionAI = ConnectionAI;
        this.id = id;
        this.seed = seed;
        this.rules = rules;

        drawDeck.shuffle(seed);

        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                player.addCardToHand(this.drawDeck.drawCard());
            }
            player.sortCards();
        }

        this.RULE_STACKING = rules[0];
        this.RULE_SKIP_N_FLIP = rules[1];
        this.RULE_SEVEN_ZERO = rules[2];
        this.RULE_FORCE_PLAY_DRAWN = rules[3];
        this.RULE_DRAW_AND_PLAY = rules[4];
        this.RULE_DRAW_TO_MATCH = rules[5];
        this.RULE_BLUFFING = rules[6];
        this.RULE_PLAY_IDENTICAL = rules[7];

        initGame();
    }

    private void initGame() {
        scramblePlayers();

        drawDeck.shuffle(seed);
        discardPile.emptyDeck();

        Card firstCard = drawDeck.drawCard();

        if (firstCard.getColor() == Color.BLACK) {
            Random random;
            if (seed == -1) {
                random = new Random();
            } else {
                random = new Random(seed);
            }
            firstCard.setColor(Color.values()[random.nextInt(4)]);
        }

        switch (firstCard.getValue()) {
            case REVERSE: invertDirection(); break;
            case SKIP: nextPlayer(); break;
            case DRAW_TWO: stackingAmount += 2; break;
            case DRAW_FOUR: stackingAmount += 4; break;
            default: break;
        }

        discardPile.placeCard(firstCard);
    }

    public void startGame() {
        networkBotFirstDecisionDelayDone = false;
        fire(listener -> listener.onGameStart(players, rules, discardPile.peekTopCard()));
    }

    public void playTurn() {
        ArrayList<Card> playableCards;

        previousPlayerHandObservation = new double[54];

        Arrays.fill(previousPlayerHandObservation, -1);

        Player currentPlayer = getCurrentPlayer();

        human = currentPlayer instanceof PlayerHuman;
        ai = currentPlayer instanceof PlayerAI;
        random = currentPlayer instanceof PlayerRandom;

        Card topCard = discardPile.peekTopCard();

        if (topCard.getValue() == Value.DRAW_FOUR && stackingAmount != 0) {
            if (RULE_BLUFFING) {
                boolean end = handleBluffChallenge();
                if (end) {
                    fire(listener -> listener.onTurnEnd(currentPlayer));
                    finishRound();
                    return;
                }
            }
        }

        if ((topCard.getValue() == Value.DRAW_TWO || topCard.getValue() == Value.DRAW_FOUR) && stackingAmount != 0) {
            if (RULE_STACKING || RULE_SKIP_N_FLIP) {
                playableCards = new ArrayList<>();
                for (Card card : currentPlayer.getCards()) {
                    if (isCardPlayable(card)) {
                        playableCards.add(card);
                    }
                }
                if (playableCards.isEmpty()) {
                    drawCards(currentPlayer, stackingAmount);
                    stackingAmount = 0;
                    if (!RULE_DRAW_AND_PLAY) {
                        fire(listener -> listener.onTurnEnd(getCurrentPlayer()));
                        finishRound();
                        return;
                    }
                } else {
                    boolean end = handleStackChoice(playableCards);
                    if (end) {
                        fire(listener -> listener.onTurnEnd(getCurrentPlayer()));
                        finishRound();
                        return;
                    }
                }
            } else {
                drawCards(currentPlayer, stackingAmount);
                stackingAmount = 0;
                if (!RULE_DRAW_AND_PLAY) {
                    fire(listener -> listener.onTurnEnd(getCurrentPlayer()));
                    finishRound();
                    return;
                }
            }
        }

        playableCards = new ArrayList<>();

        for (Card card : currentPlayer.getCards()) {
            if (isCardPlayable(card))
                playableCards.add(card);
        }

        ArrayList<Card> finalPlayable = new ArrayList<>(playableCards);

        fire(listener -> listener.onTurnStart(currentPlayer, discardPile.peekTopCard(),
                currentPlayer.getCards(), finalPlayable));

        if (canDraw()) {
            if (playableCards.isEmpty()) {
                fire(listener -> listener.onForcedDraw(getCurrentPlayer()));
                handleDrawCardsChoice();
            } else {
                handlePlayOrDrawChoice(playableCards);
            }
        } else {
            if (playableCards.isEmpty()) {
                fire(listener -> listener.onCannotPlayOrDraw(getCurrentPlayer()));
            } else {
                handlePlayWhenCannotDraw(playableCards);
            }
        }

        fire(listener -> listener.onTurnEnd(getCurrentPlayer()));

        finishRound();
    }

    private void finishRound() {
        if (debug && turnCounter%100 == 0) {
            fire(listener -> listener.onDebugTick(id, turnCounter));
        }

        turnCounter++;
        nextPlayer();

        setObservationVector(generateObservationVector());
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        copyPreviousHandIntoObs();

        if (getCurrentPlayer().getCards().isEmpty()) {
            gameOver = true;
            winner = getCurrentPlayer();

            fire(listener -> listener.onGameOver(winner));
            for (Player player : players) {
                player.onGameOver(winner);
            }
        } else if (turnCounter >= 3000) {
            gameOver = true;
            winner = null;

            fire(listener -> listener.onGameOver(null));
            for (Player player : players) {
                player.onGameOver(null);
            }
        }
    }

    private void handlePlayWhenCannotDraw(ArrayList<Card> playableCards) {
        fire(listener -> listener.onPlayWhenCannotDrawDecision(getCurrentPlayer()));

        setObservationVector(generateObservationVector());
        observationVector[FLAG_PLAY_WHEN_NO_DRAW] = 1;
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        copyPreviousHandIntoObs();
        decisionMask[MASK_BINARY_YES] = 1;
        decisionMask[MASK_BINARY_NO] = 1;
        setValidInputs(new int[]{1, 2});

        int input = getCurrentPlayer().getInput();
        input = decodeInput(input, 68);
        if (input == 0) {
            Card chosenCard = chooseCardToPlay(playableCards);
            getCurrentPlayer().discardCard(chosenCard);
            playCard(chosenCard);
        } else {
            fire(listener -> listener.onPassedTurn(getCurrentPlayer()));
        }
    }

    private void handlePlayOrDrawChoice(ArrayList<Card> playableCards) {
        fire(listener -> listener.onPlayOrDrawDecision(getCurrentPlayer()));

        setObservationVector(generateObservationVector());
        observationVector[FLAG_PLAY_OR_DRAW] = 1;
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        copyPreviousHandIntoObs();
        decisionMask[MASK_BINARY_YES] = 1;
        decisionMask[MASK_BINARY_NO] = 1;
        setValidInputs(new int[]{1, 2});

        int input = getCurrentPlayer().getInput();
        input = decodeInput(input, 68);
        if (input == 0) {
            Card chosenCard = chooseCardToPlay(playableCards);
            getCurrentPlayer().discardCard(chosenCard);
            playCard(chosenCard);
        } else {
            handleDrawCardsChoice();
        }
    }

    private boolean handleStackChoice(ArrayList<Card> playableCards) {
        fire(listener -> listener.onStackDecision(getCurrentPlayer(), stackingAmount));

        setObservationVector(generateObservationVector());
        observationVector[FLAG_STACK] = 1;
        copyPreviousHandIntoObs();
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        decisionMask[MASK_BINARY_YES] = 1;
        decisionMask[MASK_BINARY_NO] = 1;
        setValidInputs(new int[]{1, 2});

        int input = getCurrentPlayer().getInput();
        input = decodeInput(input, 68);
        if (input == 0) {
            Card chosenCard = chooseCardToPlay(playableCards);
            getCurrentPlayer().discardCard(chosenCard);
            playCard(chosenCard);
        } else {
            drawCards(getCurrentPlayer(), stackingAmount);
            stackingAmount = 0;
        }
        return !RULE_DRAW_AND_PLAY;
    }

    private boolean handleBluffChallenge() {
        fire(listener -> listener.onChallengeDecision(getCurrentPlayer()));

        setObservationVector(generateObservationVector());
        observationVector[FLAG_CHALLENGE_DRAW4] = 1;
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        decisionMask[MASK_BINARY_YES] = 1;
        decisionMask[MASK_BINARY_NO] = 1;
        copyPreviousHandIntoObs();
        setValidInputs(new int[]{1, 2});

        int input = getCurrentPlayer().getInput();
        input = decodeInput(input, 68);
        if (input == 1) {
            if (previousPlayerBluffed) {
                previousPlayerHandObservation = generatePreviousPlayerHandObservation();
                drawCards(getPreviousPlayer(), 4);
                stackingAmount -= 4;
            } else {
                drawCards(getCurrentPlayer(), stackingAmount + 6);
                stackingAmount = 0;
                if (!RULE_DRAW_AND_PLAY) {
                    nextPlayer();
                    return true;
                }
            }
        }

        return false;
    }

    private void handlePlayIdenticalCard(Card card) {
        ArrayList<Card> identicalCards = new ArrayList<>();

        for (Card c : getCurrentPlayer().getCards()) {
            if (c.getColor() == card.getColor() && c.getValue() == card.getValue()) {
                identicalCards.add(c);
            }
        }

        if (!identicalCards.isEmpty()) {
            fire(listener -> listener.onIdenticalCardDecision(getCurrentPlayer(), identicalCards));

            setObservationVector(generateObservationVector());
            observationVector[FLAG_PLAY_IDENTICAL] = 1;
            setDecisionMask(generateDecisionMask(new ArrayList<>()));
            copyPreviousHandIntoObs();
            decisionMask[MASK_BINARY_YES] = 1;
            decisionMask[MASK_BINARY_NO] = 1;
            setValidInputs(new int[]{1, 2});

            int input = getCurrentPlayer().getInput();
            input = decodeInput(input, 68);
            if (input == 0) {
                Card chosenCard = chooseCardToPlay(identicalCards);
                getCurrentPlayer().discardCard(chosenCard);
                playCard(chosenCard);
            }
        }
    }

    private void drawCards(Player player, int amount) {
        for (int i = 0; i < amount; i++) {
            if (canDraw()) {
                Card drawnCard = drawDeck.drawCard();
                player.addCardToHand(drawnCard);
                fire(listener -> listener.onCardDrawn(player, drawnCard));
            } else {
                fire(listener -> listener.onDeckEmpty());
                break;
            }
        }

        player.sortCards();
    }

    private Card chooseCardToPlay(ArrayList<Card> playableCards) {
        setObservationVector(generateObservationVector());
        observationVector[FLAG_CHOOSE_CARD] = 1;
        setDecisionMask(generateDecisionMask(playableCards));
        System.arraycopy(previousPlayerHandObservation, 0, observationVector, 54, 54);
        for (Card playableCard : playableCards) {
            decisionMask[playableCard.getId()] += 0.5;
            if (decisionMask[playableCard.getId()] > 1) {
                decisionMask[playableCard.getId()] = 1;
            }
        }
        setValidInputs(new int[playableCards.size()]);
        for (int i = 0; i < playableCards.size(); i++) {
            validInputs[i] = i;
        }
        playableCardIdsForNetwork = new int[playableCards.size()];
        for (int i = 0; i < playableCards.size(); i++) {
            playableCardIdsForNetwork[i] = playableCards.get(i).getId();
        }
        int input = getCurrentPlayer().getInput();
        if (ai) {
            int cardIndex = -1;
            for (int i = 0; i < playableCards.size(); i++) {
                if (playableCards.get(i).getId() == input) {
                    cardIndex = i;
                }
            }
            if  (cardIndex == -1) {
                throw new RuntimeException("AI returned invalid input, check the decision mask.");
            }
            return playableCards.get(cardIndex);
        }
        return playableCards.get(input);
    }

    private void handleDrawCardsChoice() {
        Card drawnCard = drawDeck.drawCard();

        if (RULE_DRAW_TO_MATCH) {
            while (canDraw() && !isCardPlayable(drawnCard)) {
                getCurrentPlayer().addCardToHand(drawnCard);
                Card finalDrawnCard = drawnCard;
                fire(listener -> listener.onCardDrawn(getCurrentPlayer(), finalDrawnCard));
                drawnCard = drawDeck.drawCard();
            }
        }

        Card finalDrawnCard = drawnCard;
        fire(listener -> listener.onCardDrawn(getCurrentPlayer(), finalDrawnCard));
        getCurrentPlayer().addCardToHand(drawnCard);

        getCurrentPlayer().sortCards();

        if (isCardPlayable(drawnCard)) {
            if (RULE_FORCE_PLAY_DRAWN) {
                getCurrentPlayer().discardCard(drawnCard);
                playCard(drawnCard);
            } else {
                Card finalDrawnCard1 = drawnCard;
                fire(listener -> listener.onPlayDrawnCardDecision(getCurrentPlayer(), finalDrawnCard1));

                setObservationVector(generateObservationVector());
                observationVector[FLAG_PLAY_DRAWN_CARD] = 1; // Set flag to indicate that the player is making a decision about whether to play the drawn card
                setDecisionMask(generateDecisionMask(new ArrayList<>()));
                copyPreviousHandIntoObs();
                decisionMask[MASK_BINARY_YES] = 1;
                decisionMask[MASK_BINARY_NO] = 1;
                setValidInputs(new int[]{1, 2});

                int input = getCurrentPlayer().getInput();
                input = decodeInput(input, 68);
                if (input == 0) {
                    getCurrentPlayer().discardCard(drawnCard);
                    playCard(drawnCard);
                } else {
                    fire(listener -> listener.onPassedTurn(getCurrentPlayer()));
                }
            }
        } else {
            Card finalDrawnCard2 = drawnCard;
            fire(listener -> listener.onDrawnCardNotPlayable(getCurrentPlayer(), finalDrawnCard2));
        }
    }

    private void copyPreviousHandIntoObs() {
        System.arraycopy(previousPlayerHandObservation, 0, observationVector, 54, 54);
    }

    private double[] generatePreviousPlayerHandObservation() {
        double[] previousPlayerHandObservation = new double[54];
        for (Card card : getPreviousPlayer().getCards()) {
            previousPlayerHandObservation[card.getId()] += 0.5;
            if (previousPlayerHandObservation[card.getId()] > 1) {
                previousPlayerHandObservation[card.getId()] = 1;
            }
        }
        return previousPlayerHandObservation;
    }

    private boolean canDraw() {
        if (drawDeck.getCards().isEmpty()) {
            reshuffleDeck();
        }
        return drawDeck.peekTopCard() != null;
    }

    private boolean isCardPlayable(Card card) {
        Card topCard = discardPile.peekTopCard();
        if (stackingAmount != 0) {
            switch (topCard.getValue()) {
                case DRAW_TWO:
                    if (RULE_SKIP_N_FLIP) {
                        return card.getValue() == Value.DRAW_TWO || card.getValue() == Value.DRAW_FOUR || card.getValue() == Value.SKIP || card.getValue() == Value.REVERSE;
                    } else {
                        return card.getValue() == Value.DRAW_TWO || card.getValue() == Value.DRAW_FOUR;
                    }
                case DRAW_FOUR:
                    if (RULE_SKIP_N_FLIP) {
                        return card.getValue() == Value.DRAW_TWO || card.getValue() == Value.DRAW_FOUR || card.getValue() == Value.SKIP || card.getValue() == Value.REVERSE;
                    } else {
                        return card.getValue() == Value.DRAW_FOUR;
                    }
            }
        }
        return card.getColor() == topCard.getColor() || card.getValue() == topCard.getValue() || card.getColor() == Color.BLACK;
    }

    private void playCard(Card card) {
        int colorChoice;
        Color chosenColor;
        Card cardCopy = new Card(card.getId());

        if (RULE_SEVEN_ZERO) {
            switch (card.getValue()) {
                case ZERO:
                    ArrayList<Card> x = new ArrayList<>(getCurrentPlayer().getCards());

                    for (int i = 0; i < players.length - 1; i++) {
                        players[i] = players[i + 1];
                    }

                    players[players.length - 1].setCards(x);

                    break;

                case SEVEN:
                    fire(listener -> listener.onSwapHandsDecision(getCurrentPlayer(), players.length));

                    setObservationVector(generateObservationVector());
                    observationVector[FLAG_SWAP_HANDS] = 1;
                    setDecisionMask(generateDecisionMask(new ArrayList<>()));
                    copyPreviousHandIntoObs();
                    for (int i = 0; i < players.length; i++) {
                        decisionMask[58 + i] = 1; // Options to swap hands with each player
                    }
                    setValidInputs(new int[players.length]);
                    for (int i = 0; i < validInputs.length; i++) {
                        validInputs[i] = i + 1;
                    }

                    int input = getCurrentPlayer().getInput();
                    input = decodeInput(input, 58);
                    ArrayList<Card> temp = new ArrayList<>(getCurrentPlayer().getCards());
                    getCurrentPlayer().setCards(new ArrayList<>(players[input].getCards()));
                    players[input].setCards(temp);

                    break;
            }
        }

        switch (card.getValue()) {
            case REVERSE:
                invertDirection();
                break;

            case SKIP:
                nextPlayer();
                break;

            case WILD:
                colorChoice = chooseColor();
                chosenColor = Color.values()[colorChoice];
                card.setColor(chosenColor);
                break;

            case DRAW_TWO:
                stackingAmount += 2;
                break;

            case DRAW_FOUR:
                if (discardPile.peekTopCard().getValue() != Value.DRAW_FOUR) {
                    for (Card c : getCurrentPlayer().getCards()) {
                        if (c.getColor() == discardPile.peekTopCard().getColor()) {
                            previousPlayerBluffed = true;
                            break;
                        }
                    }
                } else {
                    previousPlayerBluffed = false;
                }

                colorChoice = chooseColor();
                chosenColor = Color.values()[colorChoice];
                card.setColor(chosenColor);
                stackingAmount += 4;
                break;
        }

        getCurrentPlayer().sortCards();
        discardPile.placeCard(card);

        fire(listener -> listener.onCardPlayed(getCurrentPlayer(), card));

        if (RULE_PLAY_IDENTICAL) {
            ArrayList<Card> handSnapshot = new ArrayList<>(getCurrentPlayer().getCards());
            for (Card c : handSnapshot) {
                if (c.getColor() == cardCopy.getColor() && c.getValue() == cardCopy.getValue()) {
                    handlePlayIdenticalCard(c);
                }
            }
        }
    }

    private int chooseColor() {
        fire(listener -> listener.onColorChoiceDecision(getCurrentPlayer()));

        setObservationVector(generateObservationVector());
        observationVector[FLAG_CHOOSE_COLOR] = 1;
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        copyPreviousHandIntoObs();
        decisionMask[MASK_COLOR_RED] = 1;
        decisionMask[MASK_COLOR_GREEN] = 1;
        decisionMask[MASK_COLOR_BLUE] = 1;
        decisionMask[MASK_COLOR_YELLOW] = 1;
        setValidInputs(new int[]{1, 2, 3, 4});
        int input = getCurrentPlayer().getInput();

        input = decodeInput(input, 55);

        return input;
    }

    private void reshuffleDeck() {
        Card topCard = discardPile.drawCard();
        ArrayList<Card> cardsToShuffle = new ArrayList<>(discardPile.getCards());
        discardPile.emptyDeck();

        for (Card card : cardsToShuffle) {
            if (card.getValue() == Value.WILD || card.getValue() == Value.DRAW_FOUR) {
                card.setColor(Color.BLACK);
            }
            drawDeck.placeCard(card);
        }

        discardPile.placeCard(topCard);

        drawDeck.shuffle(getSeed());
    }

    private void nextPlayer() {
        currentPlayerIndex = Math.floorMod(currentPlayerIndex + direction, players.length);
    }

    private void invertDirection() {
        direction = -direction;
    }

    private void scramblePlayers() {
        Random random;
        if (seed != -1) {
            random = new Random(seed);
        } else {
            random = new Random();
        }
        for (int i = players.length - 1; i > 0; i--) {
            int randomIndex = random.nextInt(i + 1);
            Player temp = players[i];
            players[i] = players[randomIndex];
            players[randomIndex] = temp;
        }
    }

    private double[] generateObservationVector() {
        // Observation vector structure:
        // 1. Index 0-53: game.players.Player's hand (0.5 if the player has a card, 1 if the player has multiple of the same card,
        //    0 if the player doesn't have the card)
        // 2. Index 54-107: In case the player has challenged a Draw Four, information about the previous player's hand
        //    (0.5 if the player has a card, 1 if the player has multiple of the same card, 0 if the player doesn't have the card)
        // 3. Index 108-161: Top card on discard pile (1 if the card is on top of the discard pile, 0 otherwise)
        // 4. Index 162-171: Amount of cards in each opponent's hand (normalized to 0-1, -1 if player is not in the game)
        // 5. Index 172-179: Flags for the rules (1 if the rule is active, 0 otherwise)
        // 6. Index 180-188: Additional flags to indicate what decision the agent is making and in what situation
        //    (e.g. whether the agent is choosing a color for a wild card, whether the agent is deciding to play a drawn card, etc.)
        double[] observationVector = new double[OBS_SIZE];

        // 1. Index 0-53: game.players.Player's hand
        for (Card card : getCurrentPlayer().getCards()) {
            observationVector[card.getId()] += 0.5;
            if (observationVector[card.getId()] > 1) {
                observationVector[card.getId()] = 1;
            }
        }

        // 2. Index 54-107: In case the player has challenged a Draw Four, information about the previous player's hand
        // (set to 0 for now, can be updated based on the situation in the game outside of this method)

        // 3. Index 108-161: Top card on discard pile
        observationVector[discardPile.peekTopCard().getId() + 108] = 1;

        // 4. Index 162-171: Amount of cards in each opponent's hand
        for (int i = 0; i < players.length; i++) {
            observationVector[162 + i] = ((double) players[i].getCards().size()) / 20;
        }

        // 4. Index 162-171: Set -1 for players that are not in the game (in case of less than 10 players)
        for (int i = 162 + players.length; i < 172; i++) {
            observationVector[i] = -1;
        }

        // 5. Index 172-179: Flags for the rules
        for (int i = 172; i < 180; i++) {
            observationVector[i] = rules[i - 172] ? 1 : 0;
        }

        // 6. Index 180-188: Additional flags (set to 0 for now, can be updated based on the situation in the game outside of this method)

        return observationVector;
    }

    private double[] generateDecisionMask(ArrayList<Card> playableCards) {
        // Decision mask structure:
        // 1. Index 0-53: Play a card from the player's hand (1 if the player can play the card, 0 otherwise)
        // 2. Index 54-57: Choose a color for a wild card (1 if the player can choose the color, 0 otherwise)
        // 3. Index 58-67: Choose a player to swap hands with for a Seven card (1 if the player can choose the player, 0 otherwise)
        // 4. Index 68-69: Universal options for binary choices (e.g. whether to play a drawn card or not, whether to challenge a Draw Four or not, etc.) (1 if the option is available, 0 otherwise)
        double[] decisionMask = new double[70];

        // 1. Index 0-53: Play a card from hand
        for (Card card : playableCards) {
            decisionMask[card.getId()] = 1;
        }

        // 2. Index 54-57: Choose a color for a wild card (set to 0 for now, can be updated based on the situation in the game outside of this method)

        // 3. Index 58-67: Choose a player to swap hands with (set to 0 for now, can be updated based on the situation in the game outside of this method)

        // 4. Index 68-69: Universal options for binary choices (set to 0 for now, can be updated based on the situation in the game outside of this method)

        return decisionMask;
    }

    private int decodeInput(int raw, int maskOffset) {
        if (ai) {
            return raw -  maskOffset;
        } else {
            return raw - 1;
        }
    }

    /** Used to slow bot turns when at least one human plays over the network. */
    public boolean hasNetworkHumanPlayer() {
        for (Player p : players) {
            if (p instanceof PlayerNetwork) {
                return true;
            }
        }
        return false;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void fire(Consumer<GameEventListener> event) {
        if (logger != null) {
            event.accept(logger);
        }

        for (Spectator spectator : spectators) {
            event.accept(spectator);
        }

        for (Player player : players) {
            if (player instanceof GameEventListener) {
                event.accept((GameEventListener) player);
            }
        }
    }

    public double[] getObservationVector() {
        return this.observationVector;
    }

    public void setObservationVector(double[] observationVector) {
        this.observationVector = observationVector;
    }

    public double[] getDecisionMask() {
        return decisionMask;
    }

    public void setDecisionMask(double[] decisionMask) {
        this.decisionMask = decisionMask;
    }

    public int[] getValidInputs() {
        return validInputs;
    }

    public void setValidInputs(int[] validInputs) {
        this.validInputs = validInputs;
        this.playableCardIdsForNetwork = new int[0];
    }

    /** Only non-empty while the current prompt is "pick a card to play" (indices in {@link #getValidInputs()}). */
    public int[] getPlayableCardIdsForNetwork() {
        return playableCardIdsForNetwork == null
                ? new int[0] : Arrays.copyOf(playableCardIdsForNetwork, playableCardIdsForNetwork.length);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Player getCurrentPlayer() {
        return players[currentPlayerIndex];
    }

    public Player getPreviousPlayer() {
        return players[Math.floorMod(currentPlayerIndex - direction, players.length)];
    }

    public Player getNextPlayer() {
        return players[Math.floorMod(currentPlayerIndex + direction, players.length)];
    }

    public Player[] getPlayers() {
        return players;
    }

    private int getPlayerIndex(Player player) {
        for (int i = 0; i < players.length; i++) {
            if (players[i] == player) {
                return i;
            }
        }
        throw new IllegalArgumentException("Player is not part of this simulation.");
    }

    public int getHandSize(Player player) {
        return player.getCards().size();
    }

    public int getNextOpponentHandSize(Player player) {
        int index = getPlayerIndex(player);
        int nextIndex = Math.floorMod(index + direction, players.length);
        return players[nextIndex].getCards().size();
    }

    public double computeShapedReward(Player player, int previousOwnHandSize, int previousNextOpponentHandSize) {
        int currentOwnHandSize = getHandSize(player);
        int currentNextOpponentHandSize = getNextOpponentHandSize(player);

        double ownHandDelta = 0.03 * (previousOwnHandSize - currentOwnHandSize);
        double opponentPressureDelta = 0.01 * (currentNextOpponentHandSize - previousNextOpponentHandSize);
        double stepCost = -0.001;

        return ownHandDelta + opponentPressureDelta + stepCost;
    }

    public int getStackingAmount() {
        return stackingAmount;
    }

    public Card getTopCard() {
        return discardPile.peekTopCard();
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void addSpectator(Spectator spectator) {
        spectators.add(spectator);
    }
}