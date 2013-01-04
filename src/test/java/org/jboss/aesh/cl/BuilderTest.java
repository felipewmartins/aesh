/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class BuilderTest extends TestCase {

    public BuilderTest(String name) {
        super(name);
    }

    public void testBuilder() {
        ParameterBuilder pb = new ParameterBuilder();
        pb.name("foo").usage("foo is bar");
        pb.addOption(
                new OptionBuilder().description("filename given").name('f').longName("filename")
                        .hasValue(true).create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        CommandLine cl = clp.parse("foo -f test1.txt");
        assertTrue(cl.hasOption('f'));
        assertTrue(cl.hasOption("filename"));
        assertEquals("test1.txt", cl.getOptionValue('f'));
    }

    public void testBuilder2() {

        ParameterBuilder pb = new ParameterBuilder().name("less").usage("less is more");
        pb.addOption(
                new OptionBuilder().description("version").name('V').longName("version")
                        .hasValue(false).required(true).create());
        pb.addOption(
                new OptionBuilder().description("is verbose").name('v').longName("verbose")
                        .hasValue(false).create());

        pb.addOption(
                new OptionBuilder().description("attributes").name('D')
                        .isProperty(true).create());

        pb.addOption(
                new OptionBuilder().description("values").longName("values")
                        .hasMultipleValues(true).create());

        CommandLineParser clp = new ParserBuilder(pb.generateParameter()).generateParser();

        CommandLine cl = clp.parse("less -V test1.txt");
        assertTrue(cl.hasOption('V'));
        assertNull(cl.getOptionValue('V'));
        assertFalse(cl.hasOption('v'));
        assertEquals("test1.txt", cl.getArguments().get(0));

        cl = clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 test1.txt");
        assertTrue(cl.hasOption('D'));
        assertEquals("bar2", cl.getOptionProperties("D").get(1).getValue());

        cl = clp.parse("less -V -Dfoo1=bar1 -Dfoo2=bar2 --values f1,f2,f3 test1.txt");
        assertTrue(cl.hasOption("values"));
        assertEquals("f2", cl.getOptionValues("values").get(1));
        assertEquals("test1.txt", cl.getArguments().get(0));
    }
}
