/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import jdbm.RecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Exercises DIRSERVER-2102 through ApacheDS' JDBM partition wrapper classes,
 * not through direct low-level jdbm.recman test hooks.
 */
public class DIRSERVER2102JdbmConcurrencyTest
{
    private static final int THREAD_COUNT = Integer.getInteger( "dirserver2102.threadCount", 12 );
    private static final int ITERATIONS = Integer.getInteger( "dirserver2102.iterations", 250 );
    private static final int DUP_LIMIT = 4;
    private static final String DUP_KEY = "1";

    private static SchemaManager schemaManager;

    private File dbFile;
    private RecordManager recMan;
    private JdbmTable<String, String> table;
    private JdbmTable<String, String> secondTable;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = DIRSERVER2102JdbmConcurrencyTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    @Before
    public void createTable() throws Exception
    {
        dbFile = File.createTempFile( getClass().getSimpleName(), "db" );
        BaseRecordManager base = new BaseRecordManager( dbFile.getAbsolutePath() );
        recMan = new CacheRecordManager( base, new MRU( 32 ) );

        table = newDupsTable( "test" );
    }


    @After
    public void destroyTable()
    {
        closeQuietly( secondTable );
        closeQuietly( table );
        closeQuietly( recMan );

        secondTable = null;
        table = null;
        recMan = null;

        if ( dbFile != null )
        {
            String fileToDelete = dbFile.getAbsolutePath();
            new File( fileToDelete + ".db" ).delete();
            new File( fileToDelete + ".lg" ).delete();
            dbFile.delete();
        }

        dbFile = null;
    }


    /**
     * ApacheDS stores high-duplicate index keys in a redirected JDBM BTree.
     * Concurrent reads and writes through that redirected tree must not create
     * competing BTree handles over the same RecordManager state.
     */
    @Test
    public void testDuplicateBTreeAccessThroughJdbmTableIsThreadSafe() throws Exception
    {
        for ( int i = 0; i < THREAD_COUNT * ITERATIONS; i++ )
        {
            table.put( DUP_KEY, Integer.toString( i ) );
        }

        assertTrue( table.isKeyUsingBTree( DUP_KEY ) );

        runConcurrent( "jdbm-table duplicate btree access", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                String value = Integer.toString( threadIndex * ITERATIONS + iteration );

                if ( ( threadIndex & 1 ) == 0 )
                {
                    table.has( DUP_KEY, value );
                }
                else
                {
                    table.remove( DUP_KEY, value );
                    table.put( DUP_KEY, value );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * LDAP searches browse duplicate index values through ApacheDS cursors.
     * Concurrent cursor scans and updates must not expose separate JDBM BTree
     * handles that race on the same redirected duplicate-value tree.
     */
    @Test
    public void testConcurrentValueCursorAndUpdatesDoNotCorruptRedirectedBTree() throws Exception
    {
        for ( int i = 0; i < THREAD_COUNT * ITERATIONS; i++ )
        {
            table.put( DUP_KEY, Integer.toString( i ) );
        }

        assertTrue( table.isKeyUsingBTree( DUP_KEY ) );

        runConcurrent( "jdbm-table value cursor", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                String value = Integer.toString( threadIndex * ITERATIONS + iteration );

                if ( ( threadIndex & 1 ) == 0 )
                {
                    Cursor<String> cursor = table.valueCursor( DUP_KEY );

                    try
                    {
                        cursor.beforeFirst();

                        while ( cursor.next() )
                        {
                            cursor.get();
                        }
                    }
                    finally
                    {
                        cursor.close();
                    }
                }
                else
                {
                    table.remove( DUP_KEY, value );
                    table.put( DUP_KEY, value );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * LDAP index scans use DupsCursor to walk the main table and then the
     * duplicate-value tree. Concurrent table scans and duplicate updates must
     * not corrupt either BTree browser path.
     */
    @Test
    public void testConcurrentTableCursorAndUpdatesDoNotCorruptDuplicateBTree() throws Exception
    {
        for ( int key = 0; key < THREAD_COUNT; key++ )
        {
            for ( int value = 0; value < ITERATIONS; value++ )
            {
                table.put( Integer.toString( key ), Integer.toString( value ) );
            }

            assertTrue( table.isKeyUsingBTree( Integer.toString( key ) ) );
        }

        runConcurrent( "jdbm-table full duplicate cursor", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                String key = Integer.toString( threadIndex % THREAD_COUNT );
                String value = Integer.toString( iteration );

                if ( ( threadIndex & 1 ) == 0 )
                {
                    Cursor<Tuple<String, String>> cursor = table.cursor();

                    try
                    {
                        cursor.beforeFirst();

                        while ( cursor.next() )
                        {
                            cursor.get();
                        }
                    }
                    finally
                    {
                        cursor.close();
                    }
                }
                else
                {
                    table.remove( key, value );
                    table.put( key, value );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * ApacheDS demotes duplicate values from a redirected BTree back to an
     * ArrayTree when the duplicate count drops below the limit. A cursor over
     * the old redirected BTree must not race with removal of that BTree record.
     */
    @Test
    public void testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree() throws Exception
    {
        for ( int i = 0; i <= DUP_LIMIT; i++ )
        {
            table.put( DUP_KEY, Integer.toString( i ) );
        }

        assertTrue( table.isKeyUsingBTree( DUP_KEY ) );

        runConcurrent( "jdbm-table btree demotion cursor", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                String boundaryValue = Integer.toString( DUP_LIMIT );

                if ( ( threadIndex & 1 ) == 0 )
                {
                    Cursor<String> cursor = table.valueCursor( DUP_KEY );

                    try
                    {
                        cursor.beforeFirst();

                        while ( cursor.next() )
                        {
                            cursor.get();
                        }
                    }
                    finally
                    {
                        cursor.close();
                    }
                }
                else
                {
                    table.remove( DUP_KEY, boundaryValue );
                    table.put( DUP_KEY, boundaryValue );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * Two ApacheDS table objects can load the same named JDBM table from one
     * RecordManager. Their synchronized methods do not share a monitor, so the
     * underlying BTree handle sharing must remain safe.
     */
    @Test
    public void testConcurrentSharedNamedTableHandlesDoNotCorruptBTree() throws Exception
    {
        for ( int i = 0; i < THREAD_COUNT * ITERATIONS; i++ )
        {
            table.put( DUP_KEY, Integer.toString( i ) );
        }

        secondTable = newDupsTable( "test" );

        assertTrue( table.isKeyUsingBTree( DUP_KEY ) );
        assertTrue( secondTable.isKeyUsingBTree( DUP_KEY ) );

        runConcurrent( "jdbm-table shared named handles", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                String value = Integer.toString( threadIndex * ITERATIONS + iteration );
                JdbmTable<String, String> target = ( threadIndex & 2 ) == 0 ? table : secondTable;

                if ( ( threadIndex & 1 ) == 0 )
                {
                    Cursor<String> cursor = target.valueCursor( DUP_KEY );

                    try
                    {
                        cursor.beforeFirst();

                        while ( cursor.next() )
                        {
                            cursor.get();
                        }
                    }
                    finally
                    {
                        cursor.close();
                    }
                }
                else
                {
                    target.remove( DUP_KEY, value );
                    target.put( DUP_KEY, value );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * ApacheDS opens many JDBM tables and indexes on the same RecordManager.
     * Concurrent table creation exercises named-object updates plus BTree
     * allocation and commit on shared JDBM metadata pages.
     */
    @Test
    public void testConcurrentJdbmTableCreationDoesNotCorruptNamedObjects() throws Exception
    {
        runConcurrent( "jdbm-table creation", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                if ( iteration >= 60 )
                {
                    return;
                }

                JdbmTable<String, String> createdTable = null;
                String tableName = "created-" + threadIndex + '-' + iteration;

                try
                {
                    createdTable = newDupsTable( tableName );
                    createdTable.put( DUP_KEY, Integer.toString( iteration ) );
                    createdTable.sync();
                    assertTrue( recMan.getNamedObject( tableName ) != 0 );
                    assertTrue( recMan.getNamedObject( tableName + "_btree_sz" ) != 0 );
                }
                finally
                {
                    closeQuietly( createdTable );
                }

                Thread.yield();
            }
        } );
    }


    /**
     * ApacheDS can sync multiple JdbmTable instances backed by the same
     * CacheRecordManager. Each table has its own monitor, so the shared
     * TransactionManager must tolerate concurrent synchronizeLog calls.
     */
    @Test
    public void testConcurrentJdbmTableSyncDoesNotRaceTransactionLog() throws Exception
    {
        secondTable = newDupsTable( "second" );

        for ( int i = 0; i < THREAD_COUNT * ITERATIONS; i++ )
        {
            table.put( Integer.toString( i ), Integer.toString( i ) );
            secondTable.put( Integer.toString( i ), Integer.toString( i ) );
        }

        runConcurrent( "jdbm-table sync", new ConcurrentOperation()
        {
            public void run( int threadIndex, int iteration ) throws Exception
            {
                if ( ( threadIndex & 1 ) == 0 )
                {
                    String value = Integer.toString( THREAD_COUNT * ITERATIONS + threadIndex * ITERATIONS
                        + iteration );
                    table.put( value, value );
                    table.sync();
                }
                else
                {
                    String value = Integer.toString( THREAD_COUNT * ITERATIONS * 2 + threadIndex * ITERATIONS
                        + iteration );
                    secondTable.put( value, value );
                    secondTable.sync();
                }

                Thread.yield();
            }
        } );
    }


    private JdbmTable<String, String> newDupsTable( String name ) throws Exception
    {
        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        return new JdbmTable<String, String>( schemaManager, name, DUP_LIMIT, recMan,
            comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );
    }


    private void runConcurrent( String operation, final ConcurrentOperation concurrentOperation ) throws Exception
    {
        final CountDownLatch ready = new CountDownLatch( THREAD_COUNT );
        final CountDownLatch start = new CountDownLatch( 1 );
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread[] threads = new Thread[THREAD_COUNT];

        for ( int i = 0; i < THREAD_COUNT; i++ )
        {
            final int threadIndex = i;
            threads[i] = new Thread( new Runnable()
            {
                public void run()
                {
                    ready.countDown();

                    try
                    {
                        start.await();

                        for ( int iteration = 0; iteration < ITERATIONS && failure.get() == null; iteration++ )
                        {
                            concurrentOperation.run( threadIndex, iteration );
                        }
                    }
                    catch ( Throwable t )
                    {
                        failure.compareAndSet( null, t );
                    }
                }
            }, "DIRSERVER-2102-" + operation + '-' + threadIndex );

            threads[i].start();
        }

        ready.await();
        start.countDown();

        for ( int i = 0; i < THREAD_COUNT; i++ )
        {
            threads[i].join();
        }

        Throwable thrown = failure.get();

        if ( thrown != null )
        {
            throw new AssertionError(
                "Concurrent " + operation + " should not corrupt ApacheDS JDBM partition state\n"
                    + getStackTrace( thrown ), thrown );
        }
    }


    private String getStackTrace( Throwable thrown )
    {
        StringWriter writer = new StringWriter();
        thrown.printStackTrace( new PrintWriter( writer ) );
        return writer.toString();
    }


    private static interface ConcurrentOperation
    {
        void run( int threadIndex, int iteration ) throws Exception;
    }


    private void closeQuietly( Object closeable )
    {
        try
        {
            if ( closeable instanceof JdbmTable )
            {
                ( ( JdbmTable<?, ?> ) closeable ).close();
            }
            else if ( closeable instanceof RecordManager )
            {
                ( ( RecordManager ) closeable ).close();
            }
        }
        catch ( Throwable ignored )
        {
            // The test failure carries the useful signal.
        }
    }
}
