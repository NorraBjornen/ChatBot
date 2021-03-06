package root;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import datamodel.PlayerInventory;
import datamodel.PlayerState;
import datamodel.UserID;
import datasource.CloudStorageLoader;
import datasource.TestQuestionsLoader;
import db.Database;
import io.TelegramIO;
import logic.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import telegramLogic.MessageProcessor;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

@Configuration
public class SpringConfiguration{
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
        return new ChatLogic(callboard());
    }

    @Bean
    public IMessageLogic graphWalkerLogic() { return new GraphWalkerLogic(cloudLoader().getQuestionRoot()); }

    @Bean
    public CardPlayLogic cardPlayLogic() { return new CardPlayLogic(); }

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

            player.getState().switchLogic(graphWalkerLogic());
            player.set(chatLogic(), graphWalkerLogic());
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
    public PlayerState playerState(){
        return new PlayerState(cloudLoader().getQuestionRoot().getRoot(), new PlayerInventory());
    }

    @Bean
    @Scope("prototype")
    public Player player(UserID id){
        Player player = new Player(id, playerState());
        player.set(chatLogic(), graphWalkerLogic());
        player.subscribe(telegramIO(), true);
        return player;
    }
}
