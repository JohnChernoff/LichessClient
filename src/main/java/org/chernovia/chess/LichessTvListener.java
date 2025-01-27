package org.chernovia.chess;

import com.fasterxml.jackson.databind.JsonNode;

public interface LichessTvListener {
    void gameStarted(String id, JsonNode data);
    void movePlayed(String id, JsonNode data);
    void gameOver(String id, JsonNode data);
    void gameClosed(String id);
}
