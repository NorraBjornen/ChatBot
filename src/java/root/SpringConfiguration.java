package root;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import datamodel.UserID;
import datasource.CloudStorageLoader;
import datasource.TestQuestionsLoader;
import db.Database;
import io.TelegramIO;
import logic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import telegramLogic.MessageProcessor;

import java.util.concurrent.ExecutionException;

@Configuration
public class SpringConfiguration {
    @Bean
    public Database database(){
        return new Database();
    }

    @Bean
    public CloudStorageLoader cloudLoader(){
        return new CloudStorageLoader(database());
    }

    @Bean
    public Callboard callboard(){
        return new Callboard(database());
    }

    @Bean
    public ChatLogic chatLogic(){
        return new ChatLogic(testLoader(), callboard());
    }

    @Bean
    public State state() throws ExecutionException, InterruptedException {
        State state = new State();
        for (QueryDocumentSnapshot document :
                database().getFirestore()
                        .collection("state")
                        .get()
                        .get()
                        .getDocuments()) {

            Long id = Long.valueOf(document.getId());
            Player player = document.toObject(Player.class);

            if(player.getChatId() == null)
                continue;

            player.getPlayerState().setLogic(chatLogic());
            player.subscribe(telegramIO(), false);

            state.add(id, player);
        }

        return state;
    }

    @Autowired
    ApplicationContext context;

    @Bean
    public MessageProcessor messageProcessor() throws ExecutionException, InterruptedException {
        return new MessageProcessor(state(), context);
    }

    @Bean
    public BackupWorker backupWorker() throws ExecutionException, InterruptedException {
        return new BackupWorker(state(), database());
    }

    @Bean
    public TestQuestionsLoader testLoader(){
        return new TestQuestionsLoader();
    }

    @Bean
    public TelegramIO telegramIO(){
        return new TelegramIO(System.getenv("BOT_NAME"), System.getenv("BOT_TOKEN"));
    }

    @Bean
    @Scope("prototype")
    public UserID userID(){
        return new UserID();
    }

    @Bean
    @Scope("prototype")
    public Player player(UserID id){
        Player player = new Player(chatLogic(), id);
        player.subscribe(telegramIO(), true);
        return player;
    }
}
