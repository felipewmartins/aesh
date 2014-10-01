/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.man;

import org.jboss.aesh.console.man.parser.ManFileParser;
import org.jboss.aesh.util.ANSI;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManPageLoaderTest {
    @Test
    public void testParser() {
        ManFileParser parser = new ManFileParser();
        try {
            parser.setInput(new FileInputStream("src/test/resources/asciitest1.txt"));
            parser.loadPage(80);

            assertEquals("NAME", parser.getSections().get(0).getName());
            assertEquals("SYNOPSIS", parser.getSections().get(1).getName());
            assertEquals("DESCRIPTION", parser.getSections().get(2).getName());
            assertEquals("OPTIONS", parser.getSections().get(3).getName());

            assertEquals(2, parser.getSections().get(3).getParameters().size());

            assertEquals("ASCIIDOC(1)", parser.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testParser2() {
        ManFileParser parser = new ManFileParser();
        try {
            parser.setInput(new FileInputStream("src/test/resources/asciitest2.txt"));
            parser.loadPage(80);

            assertEquals(10, parser.getSections().size());

            assertEquals("NAME", parser.getSections().get(0).getName());

            List<String> out = parser.getAsList();
            assertEquals(ANSI.BOLD+"NAME"+ ANSI.DEFAULT_TEXT, out.get(0));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
