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
package org.apache.directory;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.ApacheDsService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmRdnIndex;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;


/**
 * A tool to rebuild and repair partition index files.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionPlumber
{

    /** the directory service instance */
    private ApacheDsService service;

    /** installation layout */
    private InstanceLayout instanceLayout;


    public PartitionPlumber( File instanceRoot )
    {
        this.instanceLayout = new InstanceLayout( instanceRoot );
    }


    public void start() throws Exception
    {
        service = new ApacheDsService();
        service.start( instanceLayout );
    }


    private void updateRdnIdx( JdbmRdnIndex rdnIdx, String parentId, int nbDescendant ) throws Exception
    {
        boolean isFirst = true;

        if ( parentId.equals( Partition.ROOT_ID ) )
        {
            return;
        }

        ParentIdAndRdn parent = rdnIdx.reverseLookup( parentId );

        while ( parent != null )
        {
            rdnIdx.drop( parentId );

            if ( isFirst )
            {
                parent.setNbChildren( parent.getNbChildren() + 1 );

                isFirst = false;
            }

            parent.setNbDescendants( parent.getNbDescendants() + ( nbDescendant + 1 ) );

            // Inject the modified element into the index
            rdnIdx.add( parent, parentId );

            parentId = parent.getParentId();
            parent = rdnIdx.reverseLookup( parentId );
        }
    }


    public void inspect( String partitionDn ) throws Exception
    {
        PartitionNexus nexus = service.getDirectoryService().getPartitionNexus();
        JdbmPartition p = ( JdbmPartition ) nexus.getPartition( new Dn( partitionDn ) );
        System.out.println( "Stored entry count " + p.count() );
        SchemaManager sm = nexus.getSchemaManager();

        String version = nexus.getRootDseValue( sm.lookupAttributeTypeRegistry( SchemaConstants.VENDOR_VERSION_AT ) )
            .getString();

        if ( version.equals( "2.0.0-M14" ) || !version.startsWith( "2.0.0" ) )
        {
            System.out.println( "This tool can only work on versions >= 2.0.0-M15" );
            return;
        }

        JdbmRdnIndex rdnIdx = ( JdbmRdnIndex ) p.getRdnIndex();

        Cursor<IndexEntry<ParentIdAndRdn, String>> idxCursor = rdnIdx.forwardCursor();

        System.out.println( "Inspecting data files..." );

        System.out.println( "Clearing RDN index" );

        while ( idxCursor.next() )
        {
            IndexEntry<ParentIdAndRdn, String> ie = idxCursor.get();
            rdnIdx.drop( ie.getId() );
        }

        idxCursor.close();

        Cursor<Tuple<String, Entry>> cursor = p.getMasterTable().cursor();

        int masterTableCount = 0;
        int repaired = 0;

        System.out.println( "Re-building indices..." );

        boolean ctxEntryLoaded = false;

        try
        {
            while ( cursor.next() )
            {
                masterTableCount++;
                Tuple<String, Entry> t = cursor.get();

                String id = t.getKey();

                Entry entry = t.getValue();

                {
                    repaired++;
                    String parentId = entry.get( "entryParentId" ).getString();
                    System.out
                        .println( "Read entry " + entry.getDn() + " with ID " + id + " and parent ID " + parentId );

                    ParentIdAndRdn attrVal = null;

                    // context entry has more than one RDN
                    if ( !ctxEntryLoaded )
                    {
                        Dn dn = entry.getDn();
                        if ( p.getSuffixDn().getName().startsWith( dn.getName() ) )
                        {
                            attrVal = new ParentIdAndRdn( parentId, p.getSuffixDn().getRdns() );
                            ctxEntryLoaded = true;
                        }
                    }

                    if ( attrVal == null )
                    {
                        attrVal = new ParentIdAndRdn( parentId, entry.getDn().getRdn() );
                    }

                    rdnIdx.add( attrVal, id );

                    // Update the parent's nbChildren and nbDescendants values
                    if ( parentId != Partition.ROOT_ID )
                    {
                        updateRdnIdx( rdnIdx, parentId, 0 );
                    }

                    Attribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );
                    for ( Value<?> value : objectClass )
                    {
                        String valueStr = ( String ) value.getNormValue();

                        if ( valueStr.equals( SchemaConstants.TOP_OC ) )
                        {
                            continue;
                        }

                        p.getObjectClassIndex().add( valueStr, id );
                    }

                    // Update the EntryCsn index
                    Attribute entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT );

                    p.getEntryCsnIndex().add( entryCsn.getString(), id );

                    //System.out.println( "updated CSN index for entry with ID " + id );
                    // Update the AdministrativeRole index, if needed
                    if ( entry.containsAttribute( SchemaConstants.ADMINISTRATIVE_ROLE_AT ) )
                    {
                        // We may have more than one role
                        Attribute adminRoles = entry.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT );

                        for ( Value<?> value : adminRoles )
                        {
                            p.getAdministrativeRoleIndex().add( ( String ) value.getNormValue(), id );
                        }
                        // Adds only those attributes that are indexed
                        p.getPresenceIndex().add( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID, id );
                        //System.out.println( "updated Administrative and presence indices for entry with ID " + id );
                    }

                    // Now work on the user defined userIndices
                    for ( Attribute attribute : entry )
                    {
                        AttributeType attributeType = attribute.getAttributeType();
                        String attributeOid = attributeType.getOid();

                        if ( p.hasUserIndexOn( attributeType ) )
                        {
                            Index<Object, String> idx = ( Index<Object, String> ) p.getUserIndex( attributeType );

                            // here lookup by attributeId is OK since we got attributeId from
                            // the entry via the enumeration - it's in there as is for sure

                            for ( Value<?> value : attribute )
                            {
                                idx.add( value.getNormValue(), id );
                            }

                            // Adds only those attributes that are indexed
                            p.getPresenceIndex().add( attributeOid, id );
                        }
                    }

                }
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Exiting after fetching entries " + repaired );
            throw e;
        }
        finally
        {
            cursor.close();
        }

        p.sync();

        System.out.println( "Total entries present in the partition " + masterTableCount );
        System.out.println( "Total repaired entries " + repaired );
        System.out.println( "Repair complete" );
    }


    public void exportPartition( String partitionDn, String dirPath ) throws Exception
    {
        PartitionNexus nexus = service.getDirectoryService().getPartitionNexus();
        JdbmPartition p = ( JdbmPartition ) nexus.getPartition( new Dn( partitionDn ) );

        Cursor<Tuple<String, Entry>> cursor = p.getMasterTable().cursor();

        int count = 0;

        String filePath = dirPath + File.separator + partitionDn + ".ldif";

        System.out.println( "Exporting data of " + partitionDn + " to the file " + filePath );

        FileWriter fw = new FileWriter( filePath );

        Set<String> knownIdSet = new HashSet<String>();
        
        while ( cursor.next() )
        {
            Tuple<String, Entry> t = cursor.get();

            Entry entry = t.getValue();
            
            String entryId = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();

            if( knownIdSet.contains( entryId ) )
            {
                String err = "Found an entry %s that was already read, this is possibly due to a corrupted master table, stopping exporting of entries from here to avoid duplicates";
                err = String.format( err, entryId );
                System.out.println( err );
                break;
            }
            
            knownIdSet.add( entryId );
            
            //rdnIdx.
            String ldif = LdifUtils.convertToLdif( entry );

            fw.write( ldif );
            fw.write( '\n' );
            count++;
        }

        fw.close();
        cursor.close();
        knownIdSet.clear();

        System.out.println( "exported " + count + " entries" );
    }


    public void stop() throws Exception
    {
        service.stop();
    }


    public static void main( String[] args )
    {
        String helpText = "java -jar partition-plumber.jar -d -p [-e]\n" + 
                "-d <instance-dir-path> \n" + 
                "-p <dn-of-the-partition> \n" + 
                "-e <path-to-directory> - exports the partition data to an LDIF file under this directory";

        if ( args.length < 4 )
        {
            System.out.println( helpText );
            return;
        }

        String instance = null;
        String partitionDn = null;

        String targetDir = "/tmp";
        boolean export = false;

        for ( int i = 0; i < args.length; i++ )
        {
            String a = args[i].trim();

            if ( a.equalsIgnoreCase( "-d" ) )
            {
                instance = args[++i];
            }
            else if ( a.equalsIgnoreCase( "-p" ) )
            {
                partitionDn = args[++i];
            }
            else if ( a.equalsIgnoreCase( "-e" ) )
            {
                export = true;
            }
        }

        if ( instance == null || partitionDn == null )
        {
            System.out.println( helpText );
            return;
        }

        File file = new File( instance );
        
        if ( !file.exists() )
        {
            System.out.println( "The given instance directory " + instance + " doesn't exist" );
            return;
        }

        PartitionPlumber partitionPlumber = new PartitionPlumber( file );
        try
        {
            partitionPlumber.start();
        }
        catch ( Exception e )
        {
            System.out.println( "Failed to start the tool using the given the instance location " + instance );
            e.printStackTrace();
            return;
        }

        try
        {
            partitionPlumber.inspect( partitionDn );
        }
        catch ( Exception e )
        {
            System.out.println( "Failed to inspect and recover the partition " + partitionDn );
            e.printStackTrace();
        }

        if ( export )
        {
            try
            {
                partitionPlumber.exportPartition( partitionDn, targetDir );
            }
            catch ( Exception e )
            {
                System.out.println( "Failed to export the data of partition " + partitionDn );
                e.printStackTrace();
            }
        }

        try
        {
            partitionPlumber.stop();
        }
        catch ( Exception e )
        {
            // ignore
        }
    }
}
