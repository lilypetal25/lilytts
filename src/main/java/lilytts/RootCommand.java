package lilytts;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, subcommands = {
    TextToSsmlCommand.class,
    BookToSpeechCommand.class,
    BookToSpeechAzureCommand.class,
    MergeLinesCommand.class,
    NewsToSpeechCommand.class,
    FindArticlesCommand.class
})
public class RootCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        System.out.println("Executed default RootCommand.");
        return null;
    }
    
}
