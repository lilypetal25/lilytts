package lilytts;

import picocli.CommandLine;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
