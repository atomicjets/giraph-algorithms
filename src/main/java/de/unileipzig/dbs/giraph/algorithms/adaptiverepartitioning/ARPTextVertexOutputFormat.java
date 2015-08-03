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

import org.apache.commons.lang3.StringUtils;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.TextVertexOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * Encodes the output of the {@link ARPComputation} in the following format:
 * <p/>
 * {@code <vertex-id> <partition-id> \[<partition-id>*\] [<neighbour-id>]*}
 *
 * @author Kevin Gomez (k.gomez@freenet.de)
 * @author Martin Junghanns (junghanns@informatik.uni-leipzig.de)
 */
public class ARPTextVertexOutputFormat extends
  TextVertexOutputFormat<IntWritable, ARPVertexValue, NullWritable> {
  /**
   * Tell the output format if the partition history should be printed
   * (default false).
   */
  public static final String PARTITION_HISTORY_OUTPUT =
    "partitioning.output.partitionhistory";
  /**
   * Default value for PARTITION_HISTORY_OUTPUT.
   */
  public static final boolean DEFAULT_PARTITION_HISTORY_OUTPUT = false;
  /**
   * Used for splitting the line into the main tokens (vertex id, vertex value
   */
  private static final String VALUE_TOKEN_SEPARATOR = " ";
  /**
   * Starts partition history block
   */
  private static final String LIST_BLOCK_OPEN = "[";
  /**
   * Closes partition history block
   */
  private static final String LIST_BLOCK_CLOSE = "]";
  /**
   * Used to separate partition ids in partition history block.
   */
  private static final String PARTITION_HISTORY_SEPARATOR = ",";
  /**
   * Used to decide if Partition History should be printed or not.
   */
  private boolean historyOutput;

  /**
   * @param context the information about the task
   * @return the text vertex writer to be used
   * @throws IOException
   * @throws InterruptedException
   */
  @Override
  public TextVertexWriter createVertexWriter(TaskAttemptContext context) throws
    IOException, InterruptedException {
    this.historyOutput = getConf()
      .getBoolean(PARTITION_HISTORY_OUTPUT, DEFAULT_PARTITION_HISTORY_OUTPUT);
    return new AdaptiveRepartitioningTextVertexLineWriter();
  }

  /**
   * Used to convert a {@link ARPVertexValue} to a
   * line in the output file.
   */
  private class AdaptiveRepartitioningTextVertexLineWriter extends
    TextVertexWriterToEachLine {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Text convertVertexToLine(
      Vertex<IntWritable, ARPVertexValue, NullWritable> vertex) throws
      IOException {
      // vertex id
      StringBuilder sb = new StringBuilder(vertex.getId().toString());
      sb.append(VALUE_TOKEN_SEPARATOR);
      // vertex value
      sb.append(vertex.getValue().getCurrentPartition());
      sb.append(VALUE_TOKEN_SEPARATOR);
      // vertex partition history
      if (historyOutput) {
        sb.append(LIST_BLOCK_OPEN);
        if (vertex.getValue().getPartitionHistoryCount() > 0) {
          sb.append(StringUtils.join(vertex.getValue().getPartitionHistory(),
            PARTITION_HISTORY_SEPARATOR));
        }
        sb.append(LIST_BLOCK_CLOSE);
        sb.append(VALUE_TOKEN_SEPARATOR);
      }
      // edges
      for (Edge<IntWritable, NullWritable> e : vertex.getEdges()) {
        sb.append(e.getTargetVertexId());
        sb.append(VALUE_TOKEN_SEPARATOR);
      }
      return new Text(sb.toString());
    }
  }
}
