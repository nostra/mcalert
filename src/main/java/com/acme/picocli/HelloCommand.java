package com.acme.picocli;

import io.quarkus.runtime.QuarkusApplication;
import jakarta.enterprise.context.Dependent;
import picocli.CommandLine;

//@QuarkusMain
//@CommandLine.Command
public class HelloCommand implements Runnable, QuarkusApplication {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Who will we greet?", defaultValue = "World")
    String name;

    private final GreetingService greetingService;

    public HelloCommand(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public void run() {
        greetingService.sayHello(name);
    }

    /*
    public static void main(String[] args) {
        int exit = new CommandLine(new HelloCommand(new GreetingService())).execute(args);
        System.exit(exit);
    }
    /*/
    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(new HelloCommand(new GreetingService())).execute(args);
    }//*/
}

@Dependent
class GreetingService {
    void sayHello(String name) {
        System.out.println("Hepp " + name + "!");
    }
}