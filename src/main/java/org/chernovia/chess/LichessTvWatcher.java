package org.chernovia.chess;

import chariot.Client;
import chariot.model.Enums;
import chariot.model.Fail;
import chariot.model.Many;
import chariot.model.TVFeedEvent;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import static org.chernovia.chess.LichessClient.log;

public class LichessTvWatcher extends Thread {

    LichessTvListener listener;
    Enums.Channel channel;
    TVFeedEvent.Featured currentFeatured;

    public LichessTvWatcher(LichessTvListener listener, Enums.Channel channel) {
        this.listener = listener; this.channel = channel;
    }

    public void run() {
        try {
            Client client = Client.basic();
            Many<TVFeedEvent> feed = client.games().tvFeed(channel);
            if (feed instanceof Fail<TVFeedEvent> argh) {
                listener.streamFinished(getCurrentID(),LichessTvListener.StreamCloseCode.feedFail,argh.message(),channel);
                return;
            }
            feed.stream().forEach(tvFeedEvent -> {
                if (tvFeedEvent instanceof TVFeedEvent.Fen fen) {
                    listener.newMove(fen, new Move(
                            Square.fromValue(fen.lastMove().substring(0,2).toUpperCase()),
                            Square.fromValue(fen.lastMove().substring(2,4).toUpperCase())),channel);
                    }
                else if (tvFeedEvent instanceof TVFeedEvent.Featured featured) {
                    listener.newFeature(currentFeatured,featured,channel);
                    currentFeatured = featured;
                }
            });
        } catch (Exception e) {
            listener.streamFinished(getCurrentID(),LichessTvListener.StreamCloseCode.exception,e.getMessage(),channel);
            return;
        }
        listener.streamFinished(getCurrentID(),LichessTvListener.StreamCloseCode.finished, "finished",channel);
    }

    public String getCurrentID() {
        return currentFeatured != null ? currentFeatured.id() : "-";
    }

    public TVFeedEvent.Featured getFeatured() { return currentFeatured; }
}
