package org.chernovia.chess;

import chariot.model.TVFeedEvent;
import com.github.bhlangonijr.chesslib.move.Move;

public interface LichessTvListener2 {
    enum StreamCloseCode {error,finished}
    public void newMove(TVFeedEvent.Fen fen, Move move);
    public void newFeature(TVFeedEvent.Featured featured);
    public void streamFinished(StreamCloseCode closeCode);

}
