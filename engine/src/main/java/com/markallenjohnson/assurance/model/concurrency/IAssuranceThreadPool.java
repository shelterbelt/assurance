/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 - 2023 Mark Johnson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.markallenjohnson.assurance.model.concurrency;

public interface IAssuranceThreadPool
{
    void submit(Runnable task);
    void await();
    void register();
    void unregister();
    void setNumberOfThreads(Integer numberOfThreads);

    /**
     * Stops accepting new work, waits briefly for in-flight tasks to drain,
     * then forcibly cancels anything still running. Idempotent. After
     * {@code shutdown} returns, the instance must not be reused — callers
     * that need another phase of work should construct a fresh
     * {@link IAssuranceThreadPool}.
     *
     * <p>The legacy Swing UI sidestepped lifecycle by rebuilding its Spring
     * context per scan (so each scan got a brand-new thread-pool bean). The
     * IPC layer keeps a single Spring context for the engine's lifetime, so
     * callers must take responsibility for tearing the pool down between
     * scans; otherwise the underlying {@link java.util.concurrent.Phaser}
     * accumulates state across scans and a subsequent {@link #await()}
     * blocks indefinitely.
     */
    void shutdown();
}
