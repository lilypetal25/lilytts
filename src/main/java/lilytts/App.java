package lilytts;

import picocli.CommandLine;

public class App 
{
    public static void main( String[] args )
    {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
