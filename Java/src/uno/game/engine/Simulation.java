package uno.game.engine;

import uno.game.event.GameEventListener;
import uno.game.loggers.Logger;
import uno.game.players.Player;
import uno.game.players.PlayerAI;
import uno.game.players.PlayerHerusitic;
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

    private final Player[] players;
    private final Deck drawDeck;
    private final Deck discardPile;
    private final ConnectionAI ConnectionAI;
    private final boolean[] rules;
    private final boolean RULE_STACKING;
    private final boolean RULE_SKIP_N_FLIP;
    private final boolean RULE_SEVEN_ZERO;
    private final boolean RULE_FORCE_PLAY_DRAWN;
    private final boolean RULE_DRAW_AND_PLAY;
    private final boolean RULE_DRAW_TO_MATCH;
    private final boolean RULE_BLUFFING;
    private final boolean RULE_PLAY_IDENTICAL;

    private boolean gameOver;
    private Player winner;
    private int turnCounter = 0;
    private int currentPlayerIndex = 0;
    private int direction = 1; // +1 forward, -1 backward
    private int stackingAmount = 0;
    private boolean previousPlayerBluffed = false;
    private boolean human;
    private boolean ai;
    private boolean random;
    private boolean debug = false;
    private String id;
    private int seed;
    private double[] observationVector;
    private double[] decisionMask;
    private double[] previousPlayerHandObservation;
    private int[] validInputs;
    private int[] playableCardIdsForNetwork = new int[0];
    private Logger logger = null;

    // Updated Constructor adapting the Heuristic Player
    public Simulation(int numHumanPlayers, int numAIPlayers, int numHeuristicPlayers, int numRandomPlayers, ConnectionAI ConnectionAI, boolean[] rules, String id, int seed) {
        Player[] players = new Player[numHumanPlayers + numAIPlayers + numHeuristicPlayers + numRandomPlayers];
        this.drawDeck = new Deck();
        this.discardPile = new Deck();
        this.gameOver = false;
        this.ConnectionAI = ConnectionAI;
        this.id = id;
        this.seed = seed;
        this.rules = rules;

        drawDeck.shuffle(seed);

        int idx = 0;
        for (int i = 0; i < numHumanPlayers; i++) {
            players[idx] = new PlayerHuman("H. Player " + (i + 1), this);
            for (int j = 0; j < 7; j++) {
                players[idx].addCardToHand(drawDeck.drawCard());
            }
            players[idx].sortCards();
            idx++;
        }

        for (int i = 0; i < numAIPlayers; i++) {
            players[idx] = new PlayerAI("AI. Player " + (i + 1), this, ConnectionAI);
            for (int j = 0; j < 7; j++) {
                players[idx].addCardToHand(drawDeck.drawCard());
            }
            players[idx].sortCards();
            idx++;
        }

        for (int i = 0; i < numHeuristicPlayers; i++) {
            players[idx] = new PlayerHerusitic("Heu. Player " + (i + 1), this);
            for (int j = 0; j < 7; j++) {
                players[idx].addCardToHand(drawDeck.drawCard());
            }
            players[idx].sortCards();
            idx++;
        }

        for (int i = 0; i < numRandomPlayers; i++) {
            players[idx] = new PlayerRandom("R. Player " + (i + 1), this);
            for (int j = 0; j < 7; j++) {
                players[idx].addCardToHand(drawDeck.drawCard());
            }
            players[idx].sortCards();
            idx++;
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
            if (RULE_BLUFFING && turnCounter > 0) {
                boolean end = handleBluffChallenge();
                if (end) {
                    fire(listener -> listener.onTurnEnd(currentPlayer));
                    finishRound(currentPlayer);
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
                        finishRound(currentPlayer);
                        return;
                    }
                } else {
                    boolean end = handleStackChoice(playableCards);
                    if (end) {
                        fire(listener -> listener.onTurnEnd(getCurrentPlayer()));
                        finishRound(currentPlayer);
                        return;
                    }
                }
            } else {
                drawCards(currentPlayer, stackingAmount);
                stackingAmount = 0;
                if (!RULE_DRAW_AND_PLAY) {
                    fire(listener -> listener.onTurnEnd(getCurrentPlayer()));
                    finishRound(currentPlayer);
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

        finishRound(currentPlayer);
    }

    private void finishRound(Player player) {
        if (debug && turnCounter % 100 == 0) {
            fire(listener -> listener.onDebugTick(id, turnCounter));
        }

        turnCounter++;
        nextPlayer();

        setObservationVector(generateObservationVector());
        setDecisionMask(generateDecisionMask(new ArrayList<>()));
        copyPreviousHandIntoObs();

        if (player.getCards().isEmpty()) {
            gameOver = true;
            winner = player;

            fire(listener -> listener.onGameOver(winner));
            for (Player p : players) {
                p.onGameOver(winner);
            }
        } else if (turnCounter >= 3000) {
            gameOver = true;
            winner = null;

            fire(listener -> listener.onGameOver(null));
            for (Player p : players) {
                p.onGameOver(null);
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
        } else if (input == 1) {
            fire(listener -> listener.onPassedTurn(getCurrentPlayer()));
        } else {
            throw new RuntimeException("Invalid input");
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
        } else if (input == 1) {
            handleDrawCardsChoice();
        } else {
            throw new RuntimeException("Invalid input");
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
        } else if (input == 1){
            drawCards(getCurrentPlayer(), stackingAmount);
            stackingAmount = 0;
        } else {
            throw new RuntimeException("Invalid input");
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
        if (input == 0) {
            if (previousPlayerBluffed) {
                previousPlayerHandObservation = generatePreviousPlayerHandObservation();
                drawCards(getPreviousPlayer(), 4);
                stackingAmount -= 4;
            } else {
                drawCards(getCurrentPlayer(), stackingAmount + 2);
                stackingAmount = 0;
                if (!RULE_DRAW_AND_PLAY) {
                    return true;
                }
            }
        } else if (input != 1) {
            throw new RuntimeException("Invalid input");
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
            } else if  (input != 1) {
                throw new RuntimeException("Invalid input");
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
            validInputs[i] = i + 1;
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

        return playableCards.get(input - 1);
    }

    private void handleDrawCardsChoice() {
        if (!canDraw()) {
            fire(listener -> listener.onDeckEmpty());
            return; // Abort drawing if no cards exist
        }

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
                } else if (input == 1) {
                    fire(listener -> listener.onPassedTurn(getCurrentPlayer()));
                } else {
                    throw new RuntimeException("Invalid input.");
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
        Color playedColor = card.getColor();
        Value playedValue = card.getValue();

        if (RULE_SEVEN_ZERO) {
            switch (card.getValue()) {
                case ZERO:
                    ArrayList<Card> prevHand = new ArrayList<>(players[currentPlayerIndex].getCards());

                    int idx = currentPlayerIndex;

                    for (int i = 0; i < players.length - 1; i++) {
                        int next = Math.floorMod(idx + direction, players.length);
                        ArrayList<Card> temp = new ArrayList<>(players[next].getCards());
                        players[next].setCards(prevHand);
                        prevHand = temp;
                        idx = next;
                    }

                    players[currentPlayerIndex].setCards(prevHand);

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

                    decisionMask[58 + currentPlayerIndex] = 0; // Can't swap with yourself

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
                    previousPlayerBluffed = false;
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
                if (c.getColor() == playedColor && c.getValue() == playedValue) {
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

        drawDeck.shuffle(getSeed() + turnCounter);
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
        double[] observationVector = new double[OBS_SIZE];

        // 1. Index 0-53: Player's hand
        for (Card card : getCurrentPlayer().getCards()) {
            observationVector[card.getId()] += 0.5;
            if (observationVector[card.getId()] > 1) {
                observationVector[card.getId()] = 1;
            }
        }

        // 3. Index 108-161: Top card on discard pile
        observationVector[discardPile.peekTopCard().getId() + 108] = 1;

        // Cap the mapped opponents to a maximum of 10 so it doesn't bleed into indices 172+
        int maxOpponentsToMap = Math.min(players.length, 10);

        // 4. Index 162-171: Amount of cards in each opponent's hand
        for (int i = 0; i < maxOpponentsToMap; i++) {
            int playerIndex = Math.floorMod(currentPlayerIndex + direction * (i + 1), players.length);
            observationVector[162 + i] = ((double) players[playerIndex].getCards().size()) / 20;
        }

        // 4. Index 162-171: Set -1 for players that are not in the game
        for (int i = 162 + maxOpponentsToMap; i < 172; i++) {
            observationVector[i] = -1;
        }

        // 5. Index 172-179: Flags for the rules
        for (int i = 172; i < 180; i++) {
            if (rules[i - 172]) {
                observationVector[i] = 1.0;
            } else {
                observationVector[i] = 0.0;
            }
        }

        return observationVector;
    }

    private double[] generateDecisionMask(ArrayList<Card> playableCards) {
        double[] decisionMask = new double[70];

        // 1. Index 0-53: Play a card from hand
        for (Card card : playableCards) {
            decisionMask[card.getId()] = 1;
        }

        return decisionMask;
    }

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

    public double computeShapedReward(Player player) {
        int playerHandSize = getHandSize(player);

        int smallestHand = playerHandSize;

        for (Player p : players) {
            if (p.getCards().size() < smallestHand) {
                smallestHand = p.getCards().size();
            }
        }

        double handReward = (playerHandSize - 10) * -0.01;

        double opponentPressureReward;

        if (smallestHand == playerHandSize) {
            opponentPressureReward = 0.1;
        } else {
            opponentPressureReward = (playerHandSize - smallestHand) * -0.1;
        }

        double stepCost = -0.005;

        return handReward + opponentPressureReward + stepCost;
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

    private int decodeInput(int raw, int maskOffset) {
        if (ai) {
            return raw -  maskOffset;
        } else {
            return raw - 1;
        }
    }

    private void fire(Consumer<GameEventListener> event) {
        if (logger != null) {
            event.accept(logger);
        }

        for (Player player : players) {
            if (player instanceof GameEventListener) {
                event.accept((GameEventListener) player);
            }
        }
    }
}