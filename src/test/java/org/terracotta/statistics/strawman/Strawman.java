/*
 * All content copyright Terracotta, Inc., unless otherwise indicated.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.statistics.strawman;

import java.util.Arrays;

import org.terracotta.context.ContextElement;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matchers;
import org.terracotta.context.query.Query;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.derived.LatencySampling;
import org.terracotta.statistics.derived.MinMaxAverage;
import org.terracotta.statistics.observer.ChainedEventObserver;
import org.terracotta.statistics.strawman.Cache.GetResult;

import static java.util.EnumSet.*;
import static org.terracotta.context.query.Matchers.*;
import static org.terracotta.context.query.QueryBuilder.*;

public final class Strawman {
  
  public static void main(String[] args) {
    CacheManager manager = new CacheManager("manager-one");
    Cache<String, String> cache = new Cache("cache-one");
    
    manager.addCache(cache);

    StatisticsManager stats = new StatisticsManager();
    stats.root(manager);

    Query query = queryBuilder().descendants().filter(context(Matchers.<ContextElement>allOf(identifier(subclassOf(OperationStatistic.class)), attributes(hasAttribute("name", "get"))))).build();
    System.out.println(query);
    TreeNode getStatisticNode = stats.queryForSingleton(query);
    OperationStatistic<GetResult> getStatistic = (OperationStatistic<GetResult>) getStatisticNode.getContext().attributes().get("this");
    LatencySampling<Cache.GetResult> hitLatency = new LatencySampling(of(Cache.GetResult.HIT), 1.0f);
    MinMaxAverage hitLatencyStats = new MinMaxAverage();
    hitLatency.addDerivedStatistic(hitLatencyStats);
    getStatistic.addDerivedStatistic(hitLatency);
    
    cache.get("foo");
    System.err.println("HITS        : " + getStatistic.count(GetResult.HIT));
    System.err.println("MISSES      : " + getStatistic.count(GetResult.MISS));
    System.err.println("HIT LATENCY : " + hitLatencyStats.mean());
    
    cache.put("foo", "bar");
    cache.get("foo");
    System.err.println("HITS        : " + getStatistic.count(GetResult.HIT));
    System.err.println("MISSES      : " + getStatistic.count(GetResult.MISS));
    System.err.println("HIT LATENCY : " + hitLatencyStats.mean());

    hitLatency.addDerivedStatistic(new ChainedEventObserver() {

      @Override
      public void event(long time, long ... parameters) {
        System.out.println("Event Latency : " + parameters[0]);
      }
    });
    
    cache.get("foo");
    System.err.println("HITS        : " + getStatistic.count(GetResult.HIT));
    System.err.println("MISSES      : " + getStatistic.count(GetResult.MISS));
    System.err.println("HIT LATENCY : " + hitLatencyStats.mean());
    
    getStatistic.removeDerivedStatistic(hitLatency);

    cache.get("foo");
    System.err.println("HITS        : " + getStatistic.count(GetResult.HIT));
    System.err.println("MISSES      : " + getStatistic.count(GetResult.MISS));
    System.err.println("HIT LATENCY : " + hitLatencyStats.mean());
  }
  
  public static String dumpTree(TreeNode node) {
    return dumpSubtree(0, node);
  }
  
  public static String dumpSubtree(int indent, TreeNode node) {
    char[] indentChars = new char[indent];
    Arrays.fill(indentChars, ' ');
    StringBuilder sb = new StringBuilder();
    String nodeString = node.toString();
    sb.append(indentChars).append(nodeString).append("\n");
    for (TreeNode child : node.getChildren()) {
      sb.append(dumpSubtree(indent + nodeString.length(), child));
    }
    return sb.toString();
  }
}
