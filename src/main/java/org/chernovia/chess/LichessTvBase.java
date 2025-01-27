package org.chernovia.chess;

import chariot.Client;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class LichessTvBase {
    public enum GameCrit {avgRating,flux,upset}
    public record GameRec(String id, String pgn) {} //TODO: use PGN class instead of String
    private record Credentials(String uri, String usr, String pwd, String db) {}
    private Connection conn;
    private final Credentials credentials;

    public LichessTvBase(String uri, String usr, String pwd, String db) {
        credentials = new Credentials(uri, usr, pwd, db);
        conn = connect(credentials);
    }

    public Connection getConnection() { return conn; }

    private Connection connect(final Credentials credentials) {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
            String connStr = "jdbc:mysql://" + credentials.uri +
                    "/" + credentials.db +
                    "?user=" + credentials.usr +
                    "&password=" + credentials.pwd;
            return DriverManager.getConnection(connStr);
        } catch (SQLException ex) {
            LichessTvLogger.log2File(Level.WARNING,"DataBase connection error: " + ex);
            return null;
        }
    }

    public void addGame(String id, int avgRat, int upset, int moves, int crowd, double flux, String type, String date) {
        addGame(id,avgRat,upset,moves,crowd,flux,type,date,true);
    }
    public void addGame(String id, int avgRat, int upset, int moves, int crowd, double flux, String type, String date, boolean retry) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO tv_games (id,avgRating,upset,moves,crowd,flux,type,date) VALUES (?,?,?,?,?,?,?,?)");
            ps.setString(1,id);
            ps.setInt(2,avgRat);
            ps.setInt(3,upset);
            ps.setInt(4,moves);
            ps.setInt(5,crowd);
            ps.setDouble(6,flux);
            ps.setString(7,type);
            ps.setString(8,date);
            ps.execute();
        } catch (SQLException e) {
            if (oopsHandler(e,retry) && e instanceof CommunicationsException) {
                addGame(id, avgRat, upset, moves, crowd, flux, type, date, false);
            }
        }
    }

    public List<GameRec> getTop(int n, String crit, ZonedDateTime date) {
        try {
            PreparedStatement ps;
            if (date == null) {
                 ps = conn.prepareStatement("SELECT * FROM tv_games ORDER BY " + crit + " DESC LIMIT ?");
                 ps.setInt(1,n);
            }
            else {
                ps = conn.prepareStatement("SELECT * FROM tv_games WHERE date = ? ORDER BY " + crit + " DESC LIMIT ?");
                ps.setString(1,date.format(DateTimeFormatter.ISO_DATE));
                ps.setInt(2,n);
            }
            return getGames(ps,n);
        }
        catch (SQLException e) { oopsHandler(e,false); }
        return new ArrayList<>();
    }

    public List<GameRec> getRandomGames(int n) {
        try {
            PreparedStatement ps;
            ps = conn.prepareStatement("SELECT *  FROM tv_games ORDER BY RAND() LIMIT ?");
            ps.setInt(1,n);
            return getGames(ps,n);
        }
        catch (SQLException e) { oopsHandler(e,false); }
        return new ArrayList<>();
    }

    public List<GameRec> getGames(PreparedStatement ps, int n) throws SQLException {
        Client client = Client.basic();
        List<GameRec> gameList = new ArrayList<>();
        LichessTvLogger.log("Fetching " + n + " games by: " + ps);
        ResultSet rs = ps.executeQuery();
        for (int i=0;i<n;i++) {
            if (rs.next()) {
                String id = rs.getString("id");
                String pgn = client.games().pgnByGameId(id).get().toString();
                gameList.add(new GameRec(id,pgn));
            }
        }
        return gameList;
    }

    private boolean oopsHandler(SQLException e, boolean retry) {
        if (retry) {
            LichessTvLogger.log2File(Level.WARNING,"DataBase oops: " + e);
            conn = connect(credentials);
        }
        else {
            LichessTvLogger.log("DataBase oops: " + e);
        }
        return (conn != null);
    }


}
