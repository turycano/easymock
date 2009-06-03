/*
 * Copyright 2003-2009 OFFIS, Henri Tremblay
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.easymock.tests2;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.easymock.internal.EasyMockProperties;
import org.junit.BeforeClass;
import org.junit.Test;

public class EasyMockPropertiesTest {

    @BeforeClass
    public static void setup() throws Exception {
        // Make sure to reset to prevent getting an already initialized
        // EasyMockProperties
        resetInstance();
        
        // Overload before initializing easymock.properties
        System.setProperty("easymock.b", "3");
        
        // Set a value only in System properties
        System.setProperty("easymock.f", "5");

        // Set a value not starting by "easymock."
        System.setProperty("xxx.yyy", "6");
        
        // Be wicked, set an object
        System.getProperties().put(System.class, System.class);
        System.getProperties().put("easymock.g", System.class);
                
        // Set manually a new one
        setEasyMockProperty("easymock.e", "7");

        // Set manually an old one
        setEasyMockProperty("easymock.c", "8");

        // Overload after (will be ignored)
        System.setProperty("easymock.h", "4");
    }
    
    
    @Test
    public void testGetInstance() {
        assertExpectedValue("1", "easymock.a");
        assertExpectedValue("3", "easymock.b");
        assertExpectedValue("8", "easymock.c");
        assertExpectedValue("7", "easymock.e");
        assertExpectedValue("5", "easymock.f");
        assertExpectedValue(null, "easymock.g");
        assertExpectedValue(null, "easymock.h");
        assertExpectedValue(null, "xxx.yyy");
    }

    @Test
    public void testGetProperty() {
        final EasyMockProperties instance = EasyMockProperties.getInstance();
        
        // use the default
        assertEquals("1", instance.getProperty("easymock.a", "10"));
        // don't use the default
        assertEquals("10", instance.getProperty("easymock.z", "10"));
        // null default
        assertNull(instance.getProperty("easymock.z", null));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetProperty() {
        final EasyMockProperties instance = EasyMockProperties.getInstance();

        instance.setProperty("tralala.a", null);
    }

    @Test
    public void testNoThreadContextClassLoader() throws Exception {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            resetInstance();
            
            // Remove the context class loader
            Thread.currentThread().setContextClassLoader(null);
            
            // This instance will load easymock.properties from the
            // EasyMockProperties class loader
            EasyMockProperties.getInstance();

            // And so "easymock.a" should be there
            assertExpectedValue("1", "easymock.a");
            
        } finally {
            // Whatever happens, set the initial class loader back or it'll get
            // messy
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Test
    public void testBadPropertiesFile() throws Exception {
        
        final Boolean[] close = new Boolean[1];
        
        // A ClassLoader that returns no easymock.properties
        ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {

            @Override
            public InputStream getResourceAsStream(String name) {
                if ("easymock.properties".equals(name)) {
                    return new InputStream() {
                        @Override
                        public void close() throws IOException {
                            close[0] = Boolean.TRUE;
                        }

                        @Override
                        public int read(byte[] b, int off, int len)
                                throws IOException {
                            throw new IOException("Failed!");
                        }

                        @Override
                        public int read(byte[] b) throws IOException {
                            throw new IOException("Failed!");
                        }

                        @Override
                        public int read() throws IOException {
                            throw new IOException("Failed!");
                        }
                    };
                }
                return super.getResourceAsStream(name);
            }

        };
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            resetInstance();

            // Remove the context class loader
            Thread.currentThread().setContextClassLoader(cl);

            try {
                EasyMockProperties.getInstance();
                fail("Should have an issue loading the easymock.properties file");
            } catch (RuntimeException e) {
                assertEquals("Failed to read easymock.properties file", e
                        .getMessage());
                // Make sure the thread was closed
                assertSame(Boolean.TRUE, close[0]);
            }

        } finally {
            // Whatever happens, set the initial class loader back or it'll get
            // messy
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Test
    public void testNoEasymockPropertiesFile() throws Exception {
        // A ClassLoader that returns no easymock.properties
        ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {

            @Override
            public InputStream getResourceAsStream(String name) {
                if ("easymock.properties".equals(name)) {
                    return null;
                }
                return super.getResourceAsStream(name);
            }
            
        };
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            resetInstance();
            
            // Set our class loader
            Thread.currentThread().setContextClassLoader(cl);
            
            // This instance will try to load easymock.properties with our
            // custom
            // class loader and so won't find it
            EasyMockProperties.getInstance();
            
            // And so it shouldn't find "easymock.a"
            assertExpectedValue(null, "easymock.a");
            // But "easymock.b" is still there
            assertExpectedValue("3", "easymock.b");

        } finally {
            // Whatever happens, set the initial class loader back or it'll get
            // messy
            Thread.currentThread().setContextClassLoader(old);
        }
    }


    private static void resetInstance() throws NoSuchFieldException,
            IllegalAccessException {
        // Cheat and make the singleton uninitialized
        Field field = EasyMockProperties.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static void assertExpectedValue(String expected, String key) {
        assertEquals(expected, getEasyMockProperty(key));
    }
}
