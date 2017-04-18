/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.xdbm;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An empty Cursor implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EmptyIndexCursor<K> extends AbstractIndexCursor<K>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();


    public EmptyIndexCursor()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating EmptyIndexCursor {}", this );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<K, String> element ) throws LdapException, CursorException
    {
        checkNotClosed( "before()" );
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<K, String> element ) throws LdapException, CursorException
    {
        checkNotClosed( "after()" );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed( "beforeFirst()" );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed( "afterLast()" );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed( "first()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed( "last()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed( "previous()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed( "next()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<K, String> get() throws CursorException
    {
        checkNotClosed( "get()" );
        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_703 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( String id, K indexValue ) throws Exception
    {
        checkNotClosed( "after()" );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( String id, K indexValue ) throws Exception
    {
        checkNotClosed( "after()" );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EmptyIndexCursor {}", this );
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EmptyIndexCursor {}", this );
        }

        super.close( cause );
    }
}
