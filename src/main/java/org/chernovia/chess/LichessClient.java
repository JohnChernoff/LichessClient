package org.chernovia.chess;

import chariot.Client;
import chariot.model.Enums;
import chariot.model.Fail;
import chariot.model.Game;
import chariot.model.Many;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

abstract public class LichessClient implements LichessTvListener {
    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Enums.Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentChannel(Enums.Channel currentChannel) {
        this.currentChannel = currentChannel;
    }

    String clientID;
    Client client;
    Map<String, LichessTvSock> tvClients = new HashMap<>();
    Enums.Channel currentChannel = Enums.Channel.blitz;
    long lastPurge = System.currentTimeMillis();
    static Logger logger = Logger.getLogger(LichessTvLogger.class.getName());
    static FileHandler logFileHandler;
    static File logFile;

    public LichessClient(String id) {
        clientID = id;
        client = Client.basic();
    }

    public void pause(long t) {
        try { Thread.sleep(t); } catch (InterruptedException ignore) {}
    }

    public void followGame(String id) {
        pause(1000);
        try {
            LichessTvSock client = new LichessTvSock(id,this);
            if (tvClients.put(id, client) == null) client.connect();
        }
        catch (URISyntaxException e) { log2File(Level.WARNING,"URI Augh: " + e.getMessage()); }
    }

    public boolean watchTVGames(int n, Enums.Channel channel) {
        try {
            pause(1000);
            loadTV(n,channel).stream().filter(id -> tvClients.get(id) == null).forEach(this::followGame);
            purgeCheck();
            return true;
        }
        catch (Exception oops) {
            if (oops instanceof LichessTvLogger.ChariotException) {
                log2File(Level.WARNING,"Chariot Goof: " + oops.getMessage());
            } else {
                log2File(Level.WARNING,"Unexpected Exception: " + oops.getMessage());
            }
        }
        return false;
    }

    public void purgeCheck() {
        if (System.currentTimeMillis() - lastPurge > 15 * 60 * 1000) {
            log2File(Level.INFO,"Purging: " + clientID);
            tvClients.keySet().stream().filter(k -> tvClients.get(k).isClosed()).forEach(id -> tvClients.remove(id));
            lastPurge = System.currentTimeMillis();
        }
    }

    public Set<String> loadTV(int n, Enums.Channel channel) throws LichessTvLogger.ChariotException {
        Many<Game> games = client.games().byChannel(channel,channelFilter -> channelFilter.nb(n));
        if (games instanceof Fail(int statusCode, var err)) throw
                new LichessTvLogger.ChariotException(statusCode, "(loadTV) " + err.message());
        return games.stream().map(Game::id).collect(Collectors.toSet());
    }

    public static void updateLogfile() throws IOException {
        String fileName = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE) + ".xml";
        if (logFileHandler == null || !logFile.getName().equals(fileName)) {
            logFile = new File("logs/" + fileName);
            log("Creating new log file: " + logFile.getAbsolutePath());
            if (logFile.exists() && logFile.delete()) log("Deleted old log file: " + fileName);
            if (logFile.exists() || logFile.createNewFile()) {
                log("Log file created: " + logFile.getAbsolutePath());
                if (logFileHandler != null) {
                    logger.removeHandler(logFileHandler);
                    logFileHandler.close();
                }
                logFileHandler = new FileHandler(logFile.getAbsolutePath());
                logger.addHandler(logFileHandler);
            }
        }
    }

    public static void log2File(Level level, String message) {
        try {
            updateLogfile();
            logger.log(level,message);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void log(String str) { log(str,true,false); }
    public static void log(String str, boolean cr, boolean toFile) {
        if (cr) {
            if (toFile) logger.log(Level.INFO,str);
            else System.out.println(str);
        }
        else System.out.print(str);
    }
}
