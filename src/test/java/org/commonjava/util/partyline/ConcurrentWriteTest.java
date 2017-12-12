/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.partyline;

import org.apache.commons.io.FileUtils;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.commonjava.util.partyline.UtilThreads.reader;
import static org.commonjava.util.partyline.UtilThreads.writer;

@RunWith( BMUnitRunner.class )
public class ConcurrentWriteTest
        extends AbstractBytemanTest
{
    private final int THREAD_COUNT = 5;
    /**
     * Test that locks for mutiple reads clear correctly. This will setup an script of events for
     * a single file, where:
     * <ol>
     *     <li>Multiple reads happen simultaneously, read the content, and close</li>
     *     <li>A single write at the end ensures the other locks are clear</li>
     * </ol>
     * @throws Exception
     */
    /*@formatter:off*/
    @BMRules( rules = {
            // setup the rendezvous for all threads, which will mean everything waits until all threads are started.
            @BMRule( name = "init rendezvous", targetClass = "JoinableFileManager",
                     targetMethod = "<init>",
                     targetLocation = "ENTRY",
                     action = " createRendezvous(\"begin\", " + THREAD_COUNT + ");"
                                         + "debug(\"<<<init rendezvous for begin.\")" ),

            // When we try to init a new JoinableFile for INPUT, simulate an IOException from somewhere deeper in the stack.
            @BMRule( name = "new JoinableFile error", targetClass = "JoinableFile$JoinableOutputStream", targetMethod = "close()",
                     targetLocation = "AT INVOKE JoinableFile.close()",
                    action = "debug(\"Rendezvous writing start.\"); " + "rendezvous(\"begin\"); "
                             + "debug(\"Continue writing.\");" ) } )
    /*@formatter:on*/
    @Test
    @BMUnitConfig( debug = true )
    //    @Ignore( "Inconsistent result between Maven/IDEA executions; needs to be fixed before release!" )
    public void run()
            throws Exception
    {
        final ExecutorService execs = Executors.newFixedThreadPool( THREAD_COUNT );
        final File f = temp.newFile( "child.txt" );

        final CountDownLatch latch = new CountDownLatch( THREAD_COUNT );

        final JoinableFileManager manager = new JoinableFileManager();
        manager.startReporting( 5000, 5000 );
        final long start = System.currentTimeMillis();

        for ( int i = 0; i < THREAD_COUNT; i++ )
        {
            execs.execute( writer( manager, f, latch ) );
        }


        if(!latch.await(10, TimeUnit.SECONDS)){
            Assert.fail( "Some threads blocked" );
        };
    }

}
