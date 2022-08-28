package lilytts;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, subcommands = {
    TextToSsmlCommand.class,
    BookToSpeechAzureCommand.class,
    MergeLinesCommand.class,
    NewsToSpeechAzureCommand.class
})
public class RootCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        System.out.println("Executed default RootCommand.");
        return null;
    }
    
}
