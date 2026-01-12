package io.bobba.woodsheep.core.communication.outgoing.room;

import io.bobba.woodsheep.core.communication.protocol.OutgoingMessage;

public class RoomInfoComposer extends OutgoingMessage {

  private final String roomId;
  private final String map;
  private final boolean hideBankCards;
  private final boolean privateGame;
  private final int maxPlayers;
  private final int turnTimer;
  private final int cardDiscardLimit;
  private final int pointsToWin;

  public RoomInfoComposer(
      String roomId,
      String map,
      boolean hideBankCards,
      boolean privateGame,
      int maxPlayers,
      int turnTimer,
      int cardDiscardLimit,
      int pointsToWin) {
    super("prepareRoom");
    this.roomId = roomId;
    this.map = map;
    this.hideBankCards = hideBankCards;
    this.privateGame = privateGame;
    this.maxPlayers = maxPlayers;
    this.turnTimer = turnTimer;
    this.cardDiscardLimit = cardDiscardLimit;
    this.pointsToWin = pointsToWin;
  }

  @Override
  public Object getPayload() {
    return new Payload(
        this.roomId,
        this.map,
        this.hideBankCards,
        this.privateGame,
        this.maxPlayers,
        this.turnTimer,
        this.cardDiscardLimit,
        this.pointsToWin);
  }

  private record Payload(
      String roomId,
      String map,
      boolean hideBankCards,
      boolean privateGame,
      int maxPlayers,
      int turnTimer,
      int cardDiscardLimit,
      int pointsToWin) {}
}
