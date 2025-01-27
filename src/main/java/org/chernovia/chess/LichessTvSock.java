package org.chernovia.chess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class LichessTvSock extends LichessWebSock {

    public static ObjectMapper mapper = new ObjectMapper();
    String id;
    int maxCrowd = 0;
    LichessTvListener watcher;

    public LichessTvSock(String id, LichessTvListener watcher) throws URISyntaxException {
        super(new URI("wss://socket3.lichess.org/watch/" + id + "/black/v6?sri=" + (int)(Math.random() * 999)));
        this.id = id;
        this.watcher = watcher;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LichessTvLogger.log2File(Level.FINE,"Watching: " + id);
    }

    @Override
    public void onMessage(String message) { log("received: " + message);
        try {
            JsonNode node = mapper.readTree(message); //log("Message received: " + node.get("t").textValue());
            JsonNode type = node.get("t");
            if (type != null) {
                if (type.textValue().equals("crowd")) { //log("Crowd message received: " + message);
                    int crowd = node.get("d").get("watchers").get("nb").asInt();
                    if (crowd > maxCrowd) { //log("max crowd: " + maxCrowd);
                        maxCrowd = crowd;
                    }
                }
                else if (type.textValue().equals("move")) {
                    watcher.movePlayed(id,node.get("d"));
                }
                else if (type.textValue().equals("endData")) {
                    watcher.gameOver(id,node.get("d"));
                    this.close();
                }
            }
        } catch (Exception e) {
            log("Error parsing JSON: " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        watcher.gameClosed(id);
    }

}
