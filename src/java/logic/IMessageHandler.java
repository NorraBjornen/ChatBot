package logic;

import datamodel.UserID;

public interface IMessageHandler {
    void handle(UserID userID, String message);
}
