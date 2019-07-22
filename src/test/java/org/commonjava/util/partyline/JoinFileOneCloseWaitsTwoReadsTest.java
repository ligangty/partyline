/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith( BMUnitRunner.class )
public class JoinFileOneCloseWaitsTwoReadsTest extends AbstractBytemanTest
{

    /**
     * Simulate the condition after releasing the write-lock on a file, another writing on the file is able to be proceeded.
     * this setup an script of events for one single file, where:
     * <ol>
     *     <li>Lock and then Unlock on a specific file</li>
     *     <li>Then proceed the writing on this file</li>
     * </ol>
     * @throws Exception
     */
    @BMRules( rules = {
            // wait for lockUnlock call to exit
            @BMRule( name = "openInputStream1", targetClass = "Partyline",
                     targetMethod = "openInputStream(File,long)",
                     targetLocation = "ENTRY",
                     action = "debug(\">>>wait for service enter lockUnlock.\");"
                             + "waitFor(\"lockUnlock\");"
                             + "debug(\"<<<proceed with openOutputStream.\")" ),

            // setup the trigger to signal openOutputStream when the lockUnlock exits
            @BMRule( name = "openInputStream2", targetClass = "Partyline",
                     targetMethod = "openInputStream(File,long)",
                     targetLocation = "EXIT",
                     action = "debug(\">>>wait for service enter lockUnlock.\");"
                             + "waitFor(\"lockUnlock\");"
                             + "debug(\"<<<proceed with openOutputStream.\")" ),

            @BMRule( name = "close", targetClass = "RandomAccessJF$JoinInputStream",
                     targetMethod = "close()",
                     targetLocation = "ENTRY",
                     //condition = "$2==-1",
                     action = "debug(\">>>wait for service enter openInputStream.\");"
                             + "waitFor(\"openInputStream2\");"
                             + "debug(\"<<<proceed with openInputStream.\")" ) } )

    @Test
    @BMUnitConfig( debug = true )
    public void run()
            throws Exception
    {
        final ExecutorService execs = Executors.newFixedThreadPool( 3 );
        final CountDownLatch latch = new CountDownLatch( 3 );

        final File file = temp.newFile();
        String threadName = "reader" + readers++;
    }
}
