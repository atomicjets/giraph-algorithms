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
 * along with giraph-algorithms.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unileipzig.dbs.giraph.algorithms.btgcomputation;

import com.google.common.collect.Lists;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;

import java.io.IOException;
import java.util.List;

/**
 * Graph-BSP Implementation of the Business Transaction Graph (BTG) Extraction
 * algorithm described in "BIIIG: Enabling Business Intelligence with Integrated
 * Instance Graphs". The input graph is a so called Integrated Instance Graph
 * (IIG) which contains nodes belonging to two different classes: master data
 * and transactional data.
 * <p/>
 * A BTG is a sub-graph of the IIG which has only master data nodes as boundary
 * nodes and transactional data nodes as inner nodes. The algorithm finds all
 * BTGs inside a given IIG. In the business domain, a BTG describes a specific
 * case inside a set of business cases involving master data like Employees,
 * Customers and Products and transactional data like SalesOrders, ProductOffers
 * or Purchases.
 * <p/>
 * The algorithm is based on the idea of finding connected components by
 * communicating the minimum vertex id inside a connected sub-graph and storing
 * it. The minimum id inside a sub-graph is the BTG id. Only transactional data
 * nodes are allowed to send ids, so the master data nodes work as a
 * communication barrier between BTGs. The master data nodes receive messages
 * from transactional data nodes out of BTGs in which they are involved. They
 * store the minimum incoming BTG id by vertex id in a map and when the
 * algorithm terminates, the set of unique values inside this map is the set of
 * BTG ids the master data node is involved in.
 *
 * @author Martin Junghanns (junghanns@informatik.uni-leipzig.de)
 */
public class BTGComputation extends
  BasicComputation<LongWritable, BTGVertexValue, NullWritable, BTGMessage> {

  /**
   * Returns the current minimum value. This is always the last value in the
   * list of BTG ids stored at this vertex. Initially the minimum value is the
   * vertex id.
   *
   * @param vertex The current vertex
   * @return The minimum BTG ID that vertex knows.
   */
  private long getCurrentMinValue(
    Vertex<LongWritable, BTGVertexValue, NullWritable> vertex) {
    List<Long> btgIDs = Lists.newArrayList(vertex.getValue().getGraphs());
    return (btgIDs.size() > 0) ? btgIDs.get(btgIDs.size() - 1) :
      vertex.getId().get();
  }

  /**
   * Checks incoming messages for smaller values than the current smallest
   * value.
   *
   * @param messages        All incoming messages
   * @param currentMinValue The current minimum value
   * @return The new (maybe unchanged) minimum value
   */
  private long getNewMinValue(Iterable<BTGMessage> messages,
    long currentMinValue) {
    long newMinValue = currentMinValue;
    for (BTGMessage message : messages) {
      if (message.getBtgID() < newMinValue) {
        newMinValue = message.getBtgID();
      }
    }
    return newMinValue;
  }

  /**
   * Processes vertices of type Master.
   *
   * @param vertex   The current vertex
   * @param messages All incoming messages
   */
  private void processMasterVertex(
    Vertex<LongWritable, BTGVertexValue, NullWritable> vertex,
    Iterable<BTGMessage> messages) {
    BTGVertexValue vertexValue = vertex.getValue();
    for (BTGMessage message : messages) {
      vertexValue
        .updateNeighbourBtgID(message.getSenderID(), message.getBtgID());
    }
    vertexValue.updateBtgIDs();
    // in case the vertex has no neighbours
    if (vertexValue.getGraphCount() == 0) {
      vertexValue.addGraph(vertex.getId().get());
    }
  }

  /**
   * Processes vertices of type Transactional.
   *
   * @param vertex   The current vertex
   * @param minValue All incoming messages
   */
  private void processTransactionalVertex(
    Vertex<LongWritable, BTGVertexValue, NullWritable> vertex, long minValue) {
    vertex.getValue().removeLastBtgID();
    vertex.getValue().addGraph(minValue);
    BTGMessage message = new BTGMessage();
    message.setSenderID(vertex.getId().get());
    message.setBtgID(minValue);
    sendMessageToAllEdges(vertex, message);
  }

  /**
   * The actual BTG computation.
   *
   * @param vertex   Vertex
   * @param messages Messages that were sent to this vertex in the previous
   *                 superstep.  Each message is only guaranteed to have
   * @throws java.io.IOException
   */
  @Override
  public void compute(Vertex<LongWritable, BTGVertexValue, NullWritable> vertex,
    Iterable<BTGMessage> messages) throws IOException {
    if (vertex.getValue().getVertexType() == BTGVertexType.MASTER) {
      processMasterVertex(vertex, messages);
    } else if (vertex.getValue().getVertexType() ==
      BTGVertexType.TRANSACTIONAL) {
      long currentMinValue = getCurrentMinValue(vertex);
      long newMinValue = getNewMinValue(messages, currentMinValue);
      boolean changed = currentMinValue != newMinValue;

      if (getSuperstep() == 0 || changed) {
        processTransactionalVertex(vertex, newMinValue);
      }
    }
    vertex.voteToHalt();
  }
}
