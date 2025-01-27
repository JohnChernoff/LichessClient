package org.chernovia.chess;

import chariot.Client;
import chariot.model.Enums;
import chariot.model.TVFeedEvent;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import static org.chernovia.chess.LichessClient.log;

public class LichessTvWatcher extends Thread {

    LichessTvListener2 listener;
    Enums.Channel channel;

    public LichessTvWatcher(LichessTvListener2 listener, Enums.Channel channel) {
        this.listener = listener; this.channel = channel;
    }

    public void run() {
        try {
            Client client = Client.basic();
            client.games().tvFeed(channel).stream().forEach(tvFeedEvent -> {
                if (tvFeedEvent instanceof TVFeedEvent.Fen fen) {
                    listener.newMove(fen, new Move(
                            Square.fromValue(fen.lastMove().substring(0,2).toUpperCase()),
                            Square.fromValue(fen.lastMove().substring(2,4).toUpperCase())));
                    }
                else if (tvFeedEvent instanceof TVFeedEvent.Featured featured) {
                    listener.newFeature(featured);
                }
            });
        } catch (Exception e) {
            log("Tv Watcher error: " + e.getMessage());
            listener.streamFinished(LichessTvListener2.StreamCloseCode.error);
        }
        listener.streamFinished(LichessTvListener2.StreamCloseCode.finished);
    }
}
