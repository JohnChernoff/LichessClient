package org.chernovia.chess;

import chariot.model.Enums;
import chariot.model.TVFeedEvent;
import com.github.bhlangonijr.chesslib.move.Move;

public interface LichessTvListener {
    enum StreamCloseCode {exception,finished,feedFail}
    public void newMove(TVFeedEvent.Fen fen, Move move, Enums.Channel channel);
    public void newFeature(TVFeedEvent.Featured formerFeature, TVFeedEvent.Featured newFeature, Enums.Channel channel);
    public void streamFinished(String lastId, StreamCloseCode closeCode, String reason, Enums.Channel channel);

}
