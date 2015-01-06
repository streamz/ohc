/*
 *      Copyright (C) 2014 Robert Stupp, Koeln, Germany, robert-stupp.de
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.caffinitas.ohc.jmh;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode({ Mode.AverageTime, Mode.Throughput })
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Threads(Threads.MAX)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3)
public class ProtoBenchmark
{
    private OHCache<Integer, byte[]> cache;

    @Param("2048")
    private int valueSize = 2048;
    @Param("1073741824")
    private long capacity = 1024 * 1024 * 1024;
    @Param("-1")
    private int segmentCount = -1;
    @Param("-1")
    private int hashTableSize = -1;
    @Param("1000000")
    private int keys = 1000000;
    @Param("linked")
    private String impl = "linked";

    @State(Scope.Thread)
    public static class PutState
    {
        public int key = 1;
    }

    @State(Scope.Thread)
    public static class GetState
    {
        public int key = 1;
    }

    @Setup
    public void setup() throws ClassNotFoundException
    {
        cache = OHCacheBuilder.<Integer, byte[]>newBuilder()
                              .capacity(capacity)
                              .segmentCount(segmentCount)
                              .hashTableSize(hashTableSize)
                              .type((Class<? extends OHCache>) Class.forName("org.caffinitas.ohc." + impl + ".OHCacheImpl"))
                              .keySerializer(Utils.intSerializer)
                              .valueSerializer(Utils.byteArraySerializer)
                              .build();

        for (int i = 0; i < keys; i++)
            cache.put(i, new byte[valueSize]);
    }

    @TearDown
    public void tearDown() throws IOException
    {
        cache.close();
    }

    @Benchmark
    public void getNonExisting()
    {
        cache.get(0);
    }

    @Benchmark
    public void containsNonExisting()
    {
        cache.containsKey(0);
    }

    @Benchmark
    @Threads(value = 1)
    public void putSingleThreaded(PutState state)
    {
        cache.put(state.key++, new byte[valueSize]);
        if (state.key > keys)
            state.key = 1;
    }

    @Benchmark
    public void putMultiThreaded(PutState state)
    {
        cache.put(state.key++, new byte[valueSize]);
        if (state.key > keys)
            state.key = 1;
    }

    @Benchmark
    @Threads(value = 1)
    public void getSingleThreaded(GetState state)
    {
        cache.get(state.key++);
        if (state.key > keys)
            state.key = 1;
    }

    @Benchmark
    public void getMultiThreaded(GetState state)
    {
        cache.get(state.key++);
        if (state.key > keys)
            state.key = 1;
    }
}