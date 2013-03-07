package com.yahoo.ycsb.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.yahoo.ycsb.ByteArrayByteIterator;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;

public abstract class Neo4jClientCommandsTest
{
    protected final String TABLE = "_neo4j_usertable";
    protected final String PRIMARY_KEY = "_neo4j_primary_key";

    private static Neo4jClientCommands commands;
    private static boolean setUpIsDone;

    public abstract Neo4jClientCommands getClientCommandsImpl() throws DBException;

    @BeforeClass
    public static void cleanSlate()
    {
        setUpIsDone = false;
    }

    @Before
    public void setUp() throws DBException
    {
        if ( setUpIsDone )
        {
            return;
        }

        commands = getClientCommandsImpl();
        commands.init();
        commands.clearDb();

        assertEquals( "Database should contain zero nodes", 0, commands.nodeCount() );
        doPopulate();
        assertEquals( "Database should contain two nodes", 3, commands.nodeCount() );

        setUpIsDone = true;
    }

    @Test
    public void insert() throws DBException
    {
        long nodeCountBefore = commands.nodeCount();

        Map<String, ByteIterator> values = new HashMap<String, ByteIterator>();
        values.put( "name", new StringByteIterator( "nico" ) );
        values.put( "age", new StringByteIterator( "26" ) );
        commands.insert( TABLE, "4", values );

        assertEquals( String.format( "Database contained %s nodes, should now contain %s nodes", nodeCountBefore,
                nodeCountBefore + 1 ), nodeCountBefore + 1, commands.nodeCount() );
    }

    @Test
    public void readNonExistentNode() throws DBException
    {
        assertNodeDoesNotExist( "99" );
    }

    @Test
    public void readAllFields() throws DBException
    {
        Map<String, ByteIterator> result = commands.read( TABLE, "1", null );
        result.remove( PRIMARY_KEY );
        assertEquals( 3, result.size() );
        assertEquals( "alex", result.get( "name" ).toString() );
        assertEquals( "31", result.get( "age" ).toString() );
        assertEquals( "nz", result.get( "country" ).toString() );
    }

    @Test
    public void readSomeFields() throws DBException
    {
        Set<String> values = new HashSet<String>();
        values.add( "age" );
        Map<String, ByteIterator> result = commands.read( TABLE, "1", values );
        result.remove( PRIMARY_KEY );
        assertEquals( 1, result.size() );
        assertEquals( "31", result.get( "age" ).toString() );
    }

    @Test
    public void update() throws DBException
    {
        String newName = "jacob";

        Map<String, ByteIterator> result = commands.read( TABLE, "2", null );

        String ageBefore = result.get( "age" ).toString();
        String countryBefore = result.get( "country" ).toString();

        assertEquals( "jake", result.get( "name" ).toString() );

        Map<String, ByteIterator> writeValues = new HashMap<String, ByteIterator>();
        writeValues.put( "name", new StringByteIterator( newName ) );

        commands.update( TABLE, "2", writeValues );

        result = commands.read( TABLE, "2", null );
        assertEquals( newName, result.get( "name" ).toString() );
        assertEquals( ageBefore, result.get( "age" ).toString() );
        assertEquals( countryBefore, result.get( "country" ).toString() );
    }

    @Test
    public void updateSpecialCharacters() throws DBException
    {
        String newCountry = "/\\<>():;.,1a#%$£&*?!+-=='\"";

        Map<String, ByteIterator> result = commands.read( TABLE, "2", null );

        String nameBefore = result.get( "name" ).toString();
        String ageBefore = result.get( "age" ).toString();

        assertEquals( "se", result.get( "country" ).toString() );

        Map<String, ByteIterator> writeValues = new HashMap<String, ByteIterator>();
        writeValues.put( "country", new StringByteIterator( newCountry ) );

        commands.update( TABLE, "2", writeValues );

        result = commands.read( TABLE, "2", null );
        assertEquals( nameBefore, result.get( "name" ).toString() );
        assertEquals( ageBefore, result.get( "age" ).toString() );
        assertEquals( newCountry, result.get( "country" ).toString() );
    }

    @Test
    public void delete() throws DBException
    {
        Map<String, ByteIterator> result = commands.read( TABLE, "3", null );
        assertEquals( "temp guy", result.get( "name" ).toString() );

        commands.delete( TABLE, "3" );

        assertNodeDoesNotExist( "3" );
    }

    @Ignore
    @Test
    public void deleteNonExistentNode() throws DBException
    {
        assertEquals( false, true );
    }

    @Ignore
    @Test
    public void scan()
    {
        assertEquals( false, true );
    }

    @Test
    public void byteArrayByteIteratorTest()
    {
        assertEquals( "hello", new String( new ByteArrayByteIterator( "hello".getBytes() ).toArray() ) );
        assertEquals( "hello", new StringByteIterator( "hello" ).toString() );
        assertEquals( true,
                Arrays.equals( new ByteArrayByteIterator( new byte[] { 1, 2 } ).toArray(), new byte[] { 1, 2 } ) );
    }

    @Test
    public void stringByteIteratorTest()
    {
        assertEquals( "hello", new StringByteIterator( "hello" ).toString() );
    }

    private void doPopulate() throws DBException
    {
        Map<String, ByteIterator> values = new HashMap<String, ByteIterator>();
        values.put( "name", new StringByteIterator( "alex" ) );
        values.put( "age", new StringByteIterator( "31" ) );
        values.put( "country", new StringByteIterator( "nz" ) );
        commands.insert( TABLE, "1", values );

        values = new HashMap<String, ByteIterator>();
        values.put( "name", new StringByteIterator( "jake" ) );
        values.put( "age", new StringByteIterator( "25" ) );
        values.put( "country", new StringByteIterator( "se" ) );
        commands.insert( TABLE, "2", values );

        values = new HashMap<String, ByteIterator>();
        values.put( "name", new StringByteIterator( "temp guy" ) );
        commands.insert( TABLE, "3", values );
    }

    private void assertNodeDoesNotExist( String key )
    {
        boolean readSucceeded = true;
        try
        {
            commands.read( TABLE, key, null );
            readSucceeded = true;
        }
        catch ( DBException dbe )
        {
            readSucceeded = false;
        }
        assertEquals( false, readSucceeded );
    }

}
