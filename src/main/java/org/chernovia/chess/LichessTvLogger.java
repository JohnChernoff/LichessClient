package org.chernovia.chess;

import chariot.Client;
import chariot.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.java_websocket.client.WebSocketClient;
import java.util.logging.Level;

public class LichessTvLogger extends LichessClient implements Runnable {
    LichessTvBase gameBase;
    String dbUri, dbUsr, dbPwd, dbName;
    boolean logging;
    boolean running = true;

    public static class ChariotException extends Exception {
        public ChariotException(int statusCode, String msg) {
            super("Chariot Goof: " + msg + ", status: " + statusCode);
        }
    }

    public static void main(String[] args) { //throws URISyntaxException, IOException {
        try {
            new Thread(new LichessTvLogger(args[0],args[1],args[2],args[3])).start();
        }
        catch (Exception e) {
            log2File(Level.SEVERE,"Zoiks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LichessTvLogger(String uri, String usr, String pwd, String db) {
        super("lichess_logger" + (int)(Math.random() * 999));
        dbUri = uri; dbUsr = usr; dbPwd = pwd; dbName = db;
    }

    public void reconnectToDB(long pauseTime) {
        pause(pauseTime);
        gameBase = new LichessTvBase(dbUri,dbUsr,dbPwd,dbName);
    }

    public void run() {
        try {
            while(running) {
                gameBase = new LichessTvBase(dbUri,dbUsr,dbPwd,dbName);
                clientID = "lichess_logger" + (int)(Math.random() * 999);
                client = Client.basic();
                log2File(Level.INFO,"Starting: " + clientID);
                logging = watchTVGames(10,currentChannel);
                do pause(60 * 1000); while (logging && countGames() > 0);
                log2File(Level.INFO,"Rebooting: " + clientID);
                pause(8000);
            }
        } catch (Exception e) {
            log2File(Level.SEVERE,"Zoiks: " + e.getMessage()); e.printStackTrace();
        }
    }

    public synchronized long countGames() {
        long tvGames = tvClients.values().stream().filter(WebSocketClient::isOpen).count();
        log2File(Level.INFO,"Currently watched games: " + tvGames);
        return tvGames;
    }

    public int calcAvgRating(Pgn pgn) {
        int bRat, wRat;
        String blackElo = pgn.tagMap().get("BlackElo");
        if (blackElo != null) {
            try { bRat = Integer.parseInt(blackElo); } catch (NumberFormatException e) { bRat = 0; }
        } else bRat = 0;
        String whiteElo = pgn.tagMap().get("WhiteElo");
        if (whiteElo != null) {
            try { wRat = Integer.parseInt(whiteElo); } catch (NumberFormatException e) { wRat = 0; }
        } else wRat = 0;
        return ((bRat + wRat) / 2);
    }

    public void addGameToDB(String id) throws Exception {
        One<Pgn> pgn = client.games().pgnByGameId(id);
        if (pgn instanceof Fail(int statusCode, var err)) throw
                new ChariotException(statusCode,"(pgnByGameID) " + err.message());
        One<Game> game = client.games().byGameId(id);
        if (game instanceof Fail(int statusCode, var err)) throw
                new ChariotException(statusCode,"(byGameID) " + err.message());
        if (pgn.isPresent() && game.isPresent()) {
            int avgRat = calcAvgRating(pgn.get());
            double flux = FluxCapacitor.calcFlux(game.get(), pgn.get());
            int crowd = tvClients.containsKey(id) ? tvClients.get(id).maxCrowd : 0;
            int moves = pgn.get().moveListSAN().size();
            int upset = calcUpset(game.get(), pgn.get());
            if (moves > 12) {
                log2File(Level.INFO,"Adding game: " + id + " (flux: " + flux + ", crowd: " + crowd + ", moves: " + moves + ")");
                gameBase.addGame(id, avgRat, upset, moves, crowd, flux, currentChannel.name(), pgn.get().tagMap().get("UTCDate"));
            }
            else {
                log2File(Level.INFO,"Skipping short game: " + id);
            }
            log2File(Level.INFO,"Free memory: " + Runtime.getRuntime().freeMemory() / 1024);
        }
        else {
            log("Oops: PGN/Game not found: " + id);
        }
    }

    @Override
    public void gameStarted(String id, JsonNode data) {}

    @Override
    public void movePlayed(String id, JsonNode data) {
        //log(data.toString());
    }

    @Override
    public void gameOver(String id, JsonNode data) {
        log("Game Over: " + id);
    }

    @Override
    public void gameClosed(String id) {
        log("Game Closed: " + id);
        try {
            addGameToDB(id);
            logging = watchTVGames(10,currentChannel);
        } catch (Exception e) {
            if (e instanceof ChariotException) log2File(Level.WARNING,"*** Chariot Goof ***");
            log2File(Level.WARNING,"Game insertion error (id "  +  id + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int calcUpset(Game game, Pgn pgn) {
        try {
            int bRat = Integer.parseInt(pgn.tagMap().get("BlackElo"));
            int wRat = Integer.parseInt(pgn.tagMap().get("WhiteElo"));
            Enums.Color winner = game.winner().get();
            if (winner == Enums.Color.white) return bRat - wRat;
            else if (winner == Enums.Color.black) return wRat - bRat;
            else return Math.abs(wRat - bRat)/4;
        } catch (NumberFormatException oops) { return 0; }
    }

}
