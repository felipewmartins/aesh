/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple command line parser.
 * It parses a given string based on the Parameter given and
 * returns a {@link CommandLine}
 *
 * It can also print a formatted usage/help information.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParser {

    private List<ParameterInt> params;
    private static final String EQUALS = "=";

    public CommandLineParser(List<ParameterInt> parameters) {
        params = new ArrayList<ParameterInt>();
        params.addAll(parameters);
    }

    public CommandLineParser(ParameterInt parameterInt) {
        params = new ArrayList<ParameterInt>();
        params.add(parameterInt);
    }

    public CommandLineParser(String name, String usage) {
        params = new ArrayList<ParameterInt>();
        params.add(new ParameterInt(name, usage));
    }

    public void addParameter(ParameterInt param) {
        params.add(param);
    }

    protected List<ParameterInt> getParameters() {
        return params;
    }

    /**
     * Returns a usage String based on the defined parameter and options.
     * Useful when printing "help" info etc.
     *
     */
    public String printHelp() {
        StringBuilder builder = new StringBuilder();
        for(ParameterInt param : params)
            builder.append(param.printHelp());

        return builder.toString();
    }

    /**
     * Parse a command line with the defined parameter as base of the rules.
     * If any options are found, but not defined in the parameter object an
     * IllegalArgumentException will be thrown.
     * Also, if a required option is not found or options specified with value,
     * but is not given any value an IllegalArgumentException will be thrown.
     *
     * The options found will be returned as a {@link CommandLine} object where
     * they can be queried after.
     *
     * @param line input
     * @return CommandLine
     * @throws IllegalArgumentException
     */
    public CommandLine parse(String line) throws IllegalArgumentException {
        List<String> lines = Parser.findAllWords(line);
        if(lines.size() > 0) {
            for(ParameterInt param : params) {
                if(param.getName().equals(lines.get(0)))
                    return doParse(param, lines);
            }
        }
        throw new IllegalArgumentException("param not found: "+line);
    }

    private CommandLine doParse(ParameterInt param, List<String> lines) throws IllegalArgumentException {
        param.clean();
        CommandLine commandLine = new CommandLine();
        OptionInt active = null;
        //skip first entry since that's the name of the command
        for(int i=1; i < lines.size(); i++) {
            String parseLine = lines.get(i);
            //longName
            if(parseLine.startsWith("--")) {
                //make sure that we dont have any "active" options lying around
                if(active != null)
                    throw new IllegalArgumentException("Option: "+active.getName()+" must be given a value");

                active = findLongOption(param, parseLine.substring(2));
                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= (2+active.getLongName().length()) ||
                        !parseLine.contains(EQUALS))
                        throw new IllegalArgumentException(
                                "Option "+active.getLongName()+", must be part of a property");

                    String name =
                            parseLine.substring(2+active.getLongName().length(),
                                    parseLine.indexOf(EQUALS));
                    String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);

                    commandLine.addOption(new
                            ParsedOption(active.getName(), active.getLongName(),
                            new OptionProperty(name, value)));
                    active = null;
                }
                else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                    commandLine.addOption(new ParsedOption(active.getName(), active.getLongName(), active.getValue()));
                    active = null;
                }
                else if(active == null)
                    throw new IllegalArgumentException("Option: "+parseLine+" is not a valid option for this command");
            }
            //name
            else if(parseLine.startsWith("-")) {
                //make sure that we dont have any "active" options lying around
                if(active != null)
                    throw new IllegalArgumentException("Option: "+active.getName()+" must be given a value");
                if(parseLine.length() != 2 && !parseLine.contains("="))
                    throw new IllegalArgumentException("Option: - must be followed by a valid operator");

                active = findOption(param, parseLine.substring(1));

                if(active != null && active.isProperty()) {
                    if(parseLine.length() <= 2 ||
                            !parseLine.contains(EQUALS))
                    throw new IllegalArgumentException(
                            "Option "+active.getName()+", must be part of a property");
                    String name =
                            parseLine.substring(2, // 2+char.length
                                    parseLine.indexOf(EQUALS));
                    String value = parseLine.substring( parseLine.indexOf(EQUALS)+1);

                    commandLine.addOption(new
                            ParsedOption(active.getName(), active.getLongName(),
                            new OptionProperty(name, value)));
                    active = null;
                }

                else if(active != null && (!active.hasValue() || active.getValue() != null)) {
                    commandLine.addOption(new ParsedOption(String.valueOf(active.getName()), active.getLongName(), active.getValue()));
                    active = null;
                }
                else if(active == null)
                    throw new IllegalArgumentException("Option: "+parseLine+" is not a valid option for this command");
            }
            else if(active != null) {
                if(active.hasMultipleValues()) {
                    if(parseLine.contains(String.valueOf(active.getValueSeparator()))) {
                        for(String value : parseLine.split(String.valueOf(active.getValueSeparator()))) {
                            active.addValue(value.trim());
                        }
                    }
                }
                else
                    active.addValue(parseLine);

                commandLine.addOption(new ParsedOption(active.getName(), active.getLongName(), active.getValues()));
                active = null;
            }
            //if no param is "active", we add it as an argument
            else {
                commandLine.addArgument(parseLine);
            }
        }

        //this will throw and IllegalArgumentException if needed
        checkForMissingRequiredOptions(param, commandLine);

        return commandLine;
    }

    private void checkForMissingRequiredOptions(ParameterInt param, CommandLine commandLine) throws IllegalArgumentException {
        for(OptionInt o : param.getOptions())
            if(o.isRequired()) {
                boolean found = false;
                for(ParsedOption po : commandLine.getOptions()) {
                    if(po.getName().equals(o.getName()) ||
                            po.getName().equals(o.getLongName()))
                        found = true;
                }
                if(!found)
                    throw new IllegalArgumentException("Option: "+o.getName()+" is required for this command.");
            }
    }

    private OptionInt findOption(ParameterInt param, String line) {
        OptionInt option = param.findOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithOption(line);
        //if its a property, we'll parse it later
        if(option != null && option.isProperty())
            return option;
        if(option != null) {
           String rest = line.substring(option.getName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.addValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }

    private OptionInt findLongOption(ParameterInt param, String line) {
        OptionInt option = param.findLongOption(line);
        //simplest case
        if(option != null)
            return option;

        option = param.startWithLongOption(line);
        //if its a property, we'll parse it later
        if(option != null && option.isProperty())
            return option;
        if(option != null) {
            String rest = line.substring(option.getLongName().length());
            if(rest != null && rest.length() > 1 && rest.startsWith("=")) {
                option.addValue(rest.substring(1));
                return option;
            }
        }

        return null;
    }
}
