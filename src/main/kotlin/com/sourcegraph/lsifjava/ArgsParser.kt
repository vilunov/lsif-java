package com.sourcegraph.lsifjava

import org.apache.commons.cli.*
import java.nio.file.Paths

final data class Arguments(val projectRoot: String, val outFile: String)

fun parse(args: Array<String>): Arguments {
    val options = createOptions()
    val parser = DefaultParser();
    val formatter = HelpFormatter()

    var cmd: CommandLine = try {
        parser.parse(options, args)
    } catch (e: ParseException) {
        println(e.message)
        formatter.printHelp("lsif-java", "lsif-java is an LSIF indexer for Java.\n\n", options, "")
        System.exit(1)
        null
    }!!

    if (cmd.hasOption("help")) {
        formatter.printHelp("lsif-java", "lsif-java is an LSIF indexer for Java.\n\n", options, "")
        System.exit(0)
    }

    if (cmd.hasOption("version")) {
        println("version fuck off")
        System.exit(0);
    }

    return Arguments(
        cmd.getOptionValue("projectRoot", Paths.get("").toAbsolutePath().toString()),
        cmd.getOptionValue("out", Paths.get("").toAbsolutePath().toString()+"/dump.lsif"),
    )
}

private fun createOptions(): Options {
    val options = Options()
    options.addOption(Option("help", false, "Show help"))
    options.addOption(Option("version", false, "Show version"))
    options.addOption(Option("projectRoot", false, 
        "Specifies the project root. Defaults to the current working directory"))
    options.addOption(Option("out", false, "The output file the dump is saved to"))
    return options
}