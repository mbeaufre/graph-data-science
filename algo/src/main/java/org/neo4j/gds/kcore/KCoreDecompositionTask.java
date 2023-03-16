/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.kcore;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.haa.HugeAtomicIntArray;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.concurrent.atomic.AtomicLong;

class KCoreDecompositionTask implements Runnable {

    private final Graph localGraph;
    private final HugeAtomicIntArray currentDegrees;
    private final HugeIntArray core;
    private final HugeLongArrayStack examinationStack;
    private final AtomicLong nodeIndex;
    private int smallestActiveDegree;
    private int scanningDegree;
    private final AtomicLong remainingNodes;
    private final ProgressTracker progressTracker;
    private KCoreDecompositionPhase phase;
    private final int chunkSize;

    KCoreDecompositionTask(
        Graph localGraph,
        HugeAtomicIntArray currentDegrees,
        HugeIntArray core,
        AtomicLong nodeIndex,
        AtomicLong remainingNodes,
        int chunkSize,
        ProgressTracker progressTracker
    ) {
        this.progressTracker = progressTracker;
        this.localGraph = localGraph;
        this.currentDegrees = currentDegrees;
        this.core = core;
        this.examinationStack = HugeLongArrayStack.newStack(localGraph.nodeCount());
        this.nodeIndex = nodeIndex;
        this.remainingNodes = remainingNodes;
        this.phase = KCoreDecompositionPhase.SCAN;
        this.chunkSize = chunkSize;
    }

    @Override
    public void run() {
        if (phase == KCoreDecompositionPhase.SCAN) {
            scan();
            phase = KCoreDecompositionPhase.ACT;
        } else {
            act();
            phase = KCoreDecompositionPhase.SCAN;
        }
    }

    private void scan() {

        long upperBound = localGraph.nodeCount();
        smallestActiveDegree = -1;
        long offset;
        while ((offset = nodeIndex.getAndAdd(chunkSize)) < upperBound) {
            var currentChunk = Math.min(offset + chunkSize, upperBound);
            for (long nodeId = offset; nodeId < currentChunk; nodeId++) {
                int nodeDegree = currentDegrees.get(nodeId);
                if (nodeDegree >= scanningDegree) {
                    if (nodeDegree == scanningDegree) {
                        smallestActiveDegree = nodeDegree;
                        examinationStack.push(nodeId);
                    } else if (smallestActiveDegree == -1 || smallestActiveDegree > nodeDegree) {
                        smallestActiveDegree = nodeDegree;
                    }
                }
            }
        }
    }

    void setScanningDegree(int scanningDegree) {
        this.scanningDegree = scanningDegree;
    }

    int getSmallestActiveDegree() {
        return smallestActiveDegree;
    }

    private void act() {

        long nodesExamined = 0;
        while (!examinationStack.isEmpty()) {

            long nodeId = examinationStack.pop();
            core.set(nodeId, scanningDegree);
            nodesExamined++;

            localGraph.forEachRelationship(nodeId, (s, t) -> {
                var degree = currentDegrees.getAndAdd(t, -1);
                if (degree == scanningDegree + 1) {
                    examinationStack.push(t);
                }
                return true;
            });
        }
        remainingNodes.addAndGet(-nodesExamined);
    }

    enum KCoreDecompositionPhase {
        SCAN, ACT
    }
}