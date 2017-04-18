/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm.impl.avl;


import java.util.Comparator;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.avltree.AvlSingletonOrOrderedSetCursor;
import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.AvlTreeMap;
import org.apache.directory.server.core.avltree.AvlTreeMapImpl;
import org.apache.directory.server.core.avltree.AvlTreeMapNoDupsWrapperCursor;
import org.apache.directory.server.core.avltree.KeyTupleAvlCursor;
import org.apache.directory.server.core.avltree.LinkedAvlMapNode;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.apache.directory.server.xdbm.AbstractTable;


/**
 * A Table implementation backed by in memory AVL tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTable<K, V> extends AbstractTable<K, V>
{
    private final AvlTreeMap<K, V> avl;
    private final Comparator<Tuple<K, V>> keyOnlytupleComparator;


    public AvlTable( String name, final Comparator<K> keyComparator, final Comparator<V> valueComparator,
        boolean dupsEnabled )
    {
        super( null, name, keyComparator, valueComparator );
        this.avl = new AvlTreeMapImpl<K, V>( keyComparator, valueComparator, dupsEnabled );
        allowsDuplicates = this.avl.isDupsAllowed();
        this.keyOnlytupleComparator = new Comparator<Tuple<K, V>>()
        {
            public int compare( Tuple<K, V> t0, Tuple<K, V> t1 )
            {
                return keyComparator.compare( t0.getKey(), t1.getKey() );
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        ( ( AvlTreeMapImpl ) avl ).removeAll();
    }


    /**
     * {@inheritDoc}
     */
    public long count( K key ) throws Exception
    {
        if ( key == null )
        {
            return 0L;
        }

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return 0L;
        }

        SingletonOrOrderedSet<V> val = node.getValue();

        if ( val.isOrderedSet() )
        {
            return val.getOrderedSet().getSize();
        }

        return 1L;
    }


    /**
     * {@inheritDoc}
     */
    public V get( K key ) throws LdapException
    {
        if ( key == null )
        {
            return null;
        }

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return null;
        }

        SingletonOrOrderedSet<V> val = node.getValue();

        if ( val.isOrderedSet() )
        {
            return val.getOrderedSet().getFirst().getKey();
        }

        return val.getSingleton();
    }


    /**
     * {@inheritDoc}
     */
    public long greaterThanCount( K key ) throws Exception
    {
        return avl.getSize();
    }


    /**
     * {@inheritDoc}
     */
    public boolean has( K key ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }

        return avl.find( key ) != null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean has( K key, V value ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        return avl.find( key, value ) != null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasGreaterOrEqual( K key ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }

        return avl.findGreaterOrEqual( key ) != null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasGreaterOrEqual( K key, V val ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        LinkedAvlMapNode<K, V> node = avl.findGreaterOrEqual( key );

        if ( node == null )
        {
            return false;
        }

        if ( node.getValue().isOrderedSet() )
        {
            AvlTree<V> values = node.getValue().getOrderedSet();
            return values.findGreaterOrEqual( val ) != null;
        }

        return valueComparator.compare( node.getValue().getSingleton(), val ) >= 0;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasLessOrEqual( K key ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }

        return avl.findLessOrEqual( key ) != null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasLessOrEqual( K key, V val ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }

        LinkedAvlMapNode<K, V> node = avl.findLessOrEqual( key );

        if ( node == null )
        {
            return false;
        }

        if ( node.getValue().isOrderedSet() )
        {
            AvlTree<V> values = node.getValue().getOrderedSet();
            return values.findLessOrEqual( val ) != null;
        }

        return valueComparator.compare( node.getValue().getSingleton(), val ) <= 0;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    /**
     * {@inheritDoc}
     */
    public long lessThanCount( K key ) throws Exception
    {
        return count;
    }


    /**
     * {@inheritDoc}
     */
    public void put( K key, V value ) throws Exception
    {
        if ( ( key == null ) || ( value == null ) )
        {
            return;
        }

        if ( avl.insert( key, value ) == null )
        {
            count++;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void remove( K key ) throws Exception
    {
        if ( key == null )
        {
            return;
        }

        SingletonOrOrderedSet<V> value = avl.remove( key );

        if ( value == null )
        {
            return;
        }

        if ( value.isOrderedSet() )
        {
            count -= value.getOrderedSet().getSize();
        }
        else
        {
            count--;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void remove( K key, V value ) throws Exception
    {
        if ( avl.remove( key, value ) != null )
        {
            count--;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<K, V>> cursor() throws LdapException
    {
        if ( !allowsDuplicates )
        {
            return new AvlTreeMapNoDupsWrapperCursor<K, V>(
                new AvlSingletonOrOrderedSetCursor<K, V>( avl ) );
        }

        return new AvlTableDupsCursor<K, V>( this );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<K, V>> cursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<Tuple<K, V>>();
        }

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return new EmptyCursor<Tuple<K, V>>();
        }

        if ( node.getValue().isOrderedSet() )
        {
            return new KeyTupleAvlCursor<K, V>( node.getValue().getOrderedSet(), key );
        }

        return new SingletonCursor<Tuple<K, V>>( new Tuple<K, V>( key, node.getValue().getSingleton() ),
            keyOnlytupleComparator );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<V>();
        }

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return new EmptyCursor<V>();
        }

        if ( node.getValue().isOrderedSet() )
        {
            return new AvlTreeCursor<V>( node.getValue().getOrderedSet() );
        }

        return new SingletonCursor<V>( node.getValue().getSingleton(), valueComparator );
    }


    /**
     * Returns the internal AvlTreeMap so other classes like Cursors
     * in the same package can access it.
     *
     * @return AvlTreeMap used to store Tuples
     */
    AvlTreeMap<K, V> getAvlTreeMap()
    {
        return avl;
    }
}
