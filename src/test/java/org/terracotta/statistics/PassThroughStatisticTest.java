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
package org.terracotta.statistics;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Query;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.context.query.Matchers.*;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;

/**
 *
 * @author cdennis
 */
public class PassThroughStatisticTest {

  private Callable<Number> callable = new Callable<Number>() {
    @Override
    public Number call() {
      return 12;
    }
  };

  @Test
  public void testClean() {
    StatisticsManager.createPassThroughStatistic(this, "mystat",
        Collections.<String>emptySet(), callable);

    assertTrue(PassThroughStatistic.hasStatisticsFor(this));

    StatisticsManager.nodeFor(this).clean();

    assertFalse(PassThroughStatistic.hasStatisticsFor(this));

    StatisticsManager manager = new StatisticsManager();
    manager.root(this);

    Query query = queryBuilder().descendants().filter(context(attributes(hasAttribute("name", "mystat")))).build();
    Set<TreeNode> nodes = manager.query(query);
    assertThat(nodes, IsEmptyCollection.<TreeNode>empty());
  }

  @Test
  public void testAnnotationBasedStatDetection() {
    StatisticsManager manager = new StatisticsManager();
    manager.root(new Foo());

    TreeNode foo = manager.queryForSingleton(queryBuilder().descendants().filter(context(attributes(hasAttribute("name", "foostat")))).build());
    TreeNode bar = manager.queryForSingleton(queryBuilder().descendants().filter(context(attributes(hasAttribute("name", "barstat")))).build());

    ValueStatistic<Number> fooStat = (ValueStatistic<Number>) foo.getContext().attributes().get("this");
    ValueStatistic<Number> barStat = (ValueStatistic<Number>) bar.getContext().attributes().get("this");

    assertThat(fooStat.value(), equalTo((Number) Integer.valueOf(42)));
    assertThat(barStat.value(), equalTo((Number) Long.valueOf(42L)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAnnotationBasedStatFailsWithParameter() {
    new StatisticsManager().root(new Object() {
      @Statistic(name = "foo")
      public Integer foo(String haha) {
        return 42;
      }
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAnnotationBasedStatFailsIfStatic() {
    new StatisticsManager().root(new FooStatic());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testAnnotationBasedStatFailsWithIncorrectReturn() {
    new StatisticsManager().root(new Object() {
      @Statistic(name = "foo")
      public String foo() {
        return "42";
      }
    });
  }

  static class Foo {

    @Statistic(name = "foostat")
    public Integer foo() {
      return 42;
    }

    @Statistic(name = "barstat")
    public long bar() {
      return 42L;
    }
  }
  
  static class FooStatic {
    @Statistic(name = "foo")
    public static Integer foo() {
      return 42;
    }
  }

}
