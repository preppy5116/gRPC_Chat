package sbrt.preppy.server.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sbrt.preppy.server.messages.Message;

import java.sql.*;

/**
 * Работа базы данных в режиме in-memory.
 * Основные методы: запись сообщений и показать крайнее сообщение.
 */
@Slf4j
@Component
public class DBController {
    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";
    Connection connection;

    /**
     * Подключение к базе данных H2 in-memory
     *
     * @return
     */
    private Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            log.info(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            log.info(e.getMessage());
        }
        return dbConnection;
    }

    /**
     * Подключается к in-memory базе данных и
     * создает таблицу для хранения всех сообщений сессии
     */
    public void createTableMessages() {
        connection = getDBConnection();
        PreparedStatement createPreparedStatement = null;
        String CreateQuery = "CREATE TABLE IF NOT EXISTS MESSAGES (\n" +
                "_id int NOT NULL AUTO_INCREMENT," +
                "_data TIMESTAMP NOT NULL," +
                "_author varchar(255) NOT NULL," +
                "_messageType varchar(255) NOT NULL," +
                "_text varchar(255)NOT NULL," +
                "PRIMARY KEY(_id))";
        try {
            connection.setAutoCommit(false);
            createPreparedStatement = connection.prepareStatement(CreateQuery);
            createPreparedStatement.executeUpdate();
            createPreparedStatement.close();
            connection.commit();
        } catch (SQLException e) {
            log.info("createTableMessages: Exception Message " + e.getLocalizedMessage());
        }
    }

    /**
     * Загрузить новое сообщение в базу данных
     */
    public void loadNewMessage(Message message) throws SQLException {
        PreparedStatement insertPreparedStatement = null;
        String InsertQuery = "INSERT INTO MESSAGES (_data, _author, _messagetype, _text) VALUES (?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);
            insertPreparedStatement = connection.prepareStatement(InsertQuery);
            insertPreparedStatement.setTimestamp(1, message.getTimestamp());
            insertPreparedStatement.setString(2, String.valueOf(message.getSender()));
            insertPreparedStatement.setString(3, String.valueOf(message.getMessageType()));
            insertPreparedStatement.setString(4, message.getContent());
            System.out.println(insertPreparedStatement);
            insertPreparedStatement.executeUpdate();
            insertPreparedStatement.close();
        } catch (SQLException e) {
            log.info("loadNewMessage: Exception Message " + e.getLocalizedMessage());
        } finally {
            connection.setAutoCommit(true);
        }
    }


    /**
     * Вывести все сообщения данной сессии
     */
    public void getLastMessages() {
        PreparedStatement selectPreparedStatement = null;
//        String SelectQuery = "select * from MESSAGES";
        String SelectQuery = "SELECT * FROM MESSAGES\n" +
                "        ORDER BY _id DESC\n" +
                "        LIMIT 3;";


        try {
            connection.setAutoCommit(false);

            selectPreparedStatement = connection.prepareStatement(SelectQuery);
            ResultSet rs = selectPreparedStatement.executeQuery();
            log.info("H2 In-Memory Database inserted through PreparedStatement");

            while (rs.next()) {
                log.info("Id " + rs.getInt("_id") +
                        " Timestamp " + rs.getTimestamp("_data") +
                        " Sender " + rs.getString("_author") +
                        " Type " + rs.getString("_messagetype") +
                        " Text " + rs.getString("_text"));
            }
            selectPreparedStatement.close();
        } catch (SQLException e) {
            log.info("getLastMessages: Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void closeDataBase() throws SQLException {
        connection.close();
    }
}
