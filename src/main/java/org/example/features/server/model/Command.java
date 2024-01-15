package org.example.features.server.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Command {

    STAT("STAT"),
    NAME("NAME"),
    QUIT("QUIT"),
    MESG("MESG"),
    BCST("BCST"),
    IPAD("IPAD"),
    KILL("KILL"),
    HELP("HELP"),
    UNKNOWN("UNKNOWN")

    // placeholder
    ;


    private final String name;

    private static final Map<String, Command> INSTANCES = Stream.of(Command.values())
            .collect(Collectors.toUnmodifiableMap(Command::getName, Function.identity()));

    Command(String name) {
        this.name = name;
    }

    public static Command of(String name) {
        return INSTANCES.getOrDefault(name, UNKNOWN);
    }

    public static String getNames() {
        return String.join(", ", INSTANCES.keySet());
    }

    public String getName() {
        return name;
    }

    public static boolean exists(String name) {
        return INSTANCES.containsKey(name);
    }

}
