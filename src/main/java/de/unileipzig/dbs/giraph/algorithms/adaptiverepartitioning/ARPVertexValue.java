/*
 * This file is part of giraph-algorithms.
 *
 * giraph-algorithms is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * giraph-algorithms is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with giraph-algorithms. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unileipzig.dbs.giraph.algorithms.adaptiverepartitioning;

import com.google.common.collect.Lists;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * Custom vertex used by {@link ARPComputation}.
 *
 * @author Kevin Gomez (k.gomez@freenet.de)
 * @author Martin Junghanns (junghanns@informatik.uni-leipzig.de)
 */
public class ARPVertexValue implements Writable {
  /**
   * The desired partition the vertex want to migrate to.
   */
  private int desiredPartition;
  /**
   * The actual partition.
   */
  private int currentPartition;
  /**
   * Contains the partition history of the vertex.
   */
  private List<Integer> partitionHistory;

  /**
   * Method to set the current partition
   *
   * @param currentPartition current partition
   */
  public void setCurrentPartition(IntWritable currentPartition) {
    this.currentPartition = currentPartition.get();
  }

  /**
   * Method to set the lastValue of the vertex
   *
   * @param desiredPartition the desired Partition
   */
  public void setDesiredPartition(IntWritable desiredPartition) {
    this.desiredPartition = desiredPartition.get();
  }

  /**
   * Get method to get the desired partition
   *
   * @return the desired Partition
   */
  public IntWritable getDesiredPartition() {
    return new IntWritable(this.desiredPartition);
  }

  /**
   * Get the current partition
   *
   * @return the current partition
   */
  public IntWritable getCurrentPartition() {
    return new IntWritable(this.currentPartition);
  }

  /**
   * Get the partition history of the vertex.
   *
   * @return partitionHistory list
   */
  public List<Integer> getPartitionHistory() {
    return this.partitionHistory;
  }

  /**
   * Add a partition to the vertex partition history.
   *
   * @param partition partition id
   */
  public void addToPartitionHistory(int partition) {
    initList();
    this.partitionHistory.add(partition);
  }

  /**
   * Returns the size of the partition history.
   *
   * @return size of partition history
   */
  public int getPartitionHistoryCount() {
    return (partitionHistory != null) ? partitionHistory.size() : 0;
  }

  /**
   * Initialize the partition history
   */
  private void initList() {
    if (partitionHistory == null) {
      this.partitionHistory = Lists.newArrayList();
    }
  }

  /**
   * Serializes the content of the vertex object.
   *
   * @param dataOutput data to be serialized
   * @throws IOException
   */
  @Override
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(this.desiredPartition);
    dataOutput.writeInt(this.currentPartition);
    if (partitionHistory == null || partitionHistory.isEmpty()) {
      dataOutput.writeInt(0);
    } else {
      dataOutput.writeInt(partitionHistory.size());
      for (Integer partitions : partitionHistory) {
        dataOutput.writeInt(partitions);
      }
    }
  }

  /**
   * Deserializes the content of the vertex object.
   *
   * @param dataInput data to be deserialized
   * @throws IOException
   */
  @Override
  public void readFields(DataInput dataInput) throws IOException {
    this.desiredPartition = dataInput.readInt();
    this.currentPartition = dataInput.readInt();
    final int partitionHistorySize = dataInput.readInt();
    if (partitionHistorySize > 0) {
      initList();
    }
    for (int i = 0; i < partitionHistorySize; i++) {
      partitionHistory.add(dataInput.readInt());
    }
  }
}